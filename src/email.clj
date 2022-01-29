(ns email
  (:import [java.io PushbackReader]
           [java.io StringReader])
  (:require [clojure.edn :as edn]
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

(defn messages
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
    (if-not (contains? #{0 4} exit) ; 0 matches, 2 no matches
      (throw (ex-info "mu returned non-zero exit code" result))
      (let [reader (-> out (StringReader.) (PushbackReader.))]
        (->> (edn-seq reader)
             (map parse-message))))))

(defn reply?
  "Returns true if a message is a reply to another message."
  [message]
  (let [{:keys [subject]} message]
    (string/starts-with? subject "Re: ")))

(defn -main
  [& _]
  (let [format-message (fn [{[{:keys [name email]}] :from subject :subject}]
                         (let [email (zane.string/truncate email 20)
                               subject (zane.string/truncate-words subject 40)]
                           (str name " <" email "> " subject)))]
    (when (network/up?)
      (let [messages (remove reply? (messages))
            message-count (count messages)]
        (when (pos? message-count)
          (println (bitbar/line message-count {:sfimage "envelope"}))
          (println bitbar/separator)
          (doseq [message messages]
            (println (bitbar/line (format-message message) {:sfimage "envelope"})))
          (println bitbar/separator)
          (println (bitbar/line "Sync" {:bash "mbsync"
                                        :params ["gmail-inbox"]
                                        :terminal true
                                        :refresh true})))))))

(comment

  (messages)

  ,)
