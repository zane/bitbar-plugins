(ns email
  (:import [java.io PushbackReader]
           [java.io StringReader])
  (:require [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [io.zane.bitbar :as bitbar]
            [io.zane.network :as network]
            [io.zane.string :as zane.string]))

(defn edn-seq
  [reader]
  (let [next (edn/read {:eof ::end} reader)]
    (when-not (= next ::end)
      (cons next (lazy-seq (edn-seq reader))))))

(defn himalaya-messages
  []
  (match/match (shell/sh "himalaya" "-o" "json" "list")
    {:exit 0 :out out}
    (-> (json/parse-string out true)
        (:response))))

(defn maildir-messages
  []
  (let [parse-email (fn parse-email [[name _ email]]
                      (cond-> {}
                        (some? name) (assoc :name name)
                        (some? email) (assoc :email email)))
        parse-emails (fn parse-emails [sexp]
                       (mapv parse-email sexp))
        parse-flags (fn parse-flags [sexp]
                      (mapv keyword sexp))
        parse-priority (fn parse-priority [sexp]
                         (keyword sexp))
        parse-message (fn parse-message [sexp]
                        (-> (apply hash-map sexp)
                            (update :from parse-emails)
                            (update :to parse-emails)
                            (update :flags parse-flags)
                            (update :priority parse-priority)))
        {:keys [exit out] :as result} (shell/sh "mu" "find" "-t" "maildir:/Inbox" "--format" "sexp")]
    (if-not (contains? #{0 4} exit)     ; 0 matches, 2 no matches
      (throw (ex-info "mu returned non-zero exit code" result))
      (let [reader (-> out (StringReader.) (PushbackReader.))]
        (->> (edn-seq reader)
             (map parse-message))))))

(defn maildir-reply?
  "Returns true if a message is a reply to another message."
  [message]
  (let [{:keys [subject]} message]
    (string/starts-with? subject "Re: ")))

(defn -main
  [& _]
  (let [format-message (fn [{:keys [subject sender]}]
                         (let [sender (zane.string/truncate sender 40)
                               subject (zane.string/truncate-words subject 40)]
                           (str sender " - " subject)))]
    (when (network/up?)
      (let [messages (himalaya-messages)
            message-count (count messages)]
        (when (pos? message-count)
          (println (bitbar/line message-count {:sfimage "envelope"}))
          (println bitbar/separator)
          (doseq [message messages]
            (println (bitbar/line (format-message message) {:sfimage "envelope"}))))))))

(comment

  (himalaya-messages)
  (maildir-messages)
  (-main)

  ,)
