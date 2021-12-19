(ns io.zane.app
  (:import [java.io BufferedReader]
           [java.io StringReader])
  (:require [clojure.core.match :as match]
            [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defn ^:private processes
  []
  (let [[headers & rows] (->> (line-seq (BufferedReader. (StringReader. (:out (shell/sh "ps" "aux")))))
                              (map #(string/split % #"\s+")))]
    (map #(zipmap headers %)
         rows)))

(defn running?
  [app]
  (->> (processes)
       (some #(when-let [[_ _ actual-name]
                           (re-matches #"/Applications/([^/]+/)?([^/]+)\.app(/.*)?"
                                       (get % "COMMAND" ""))]
                  (= app actual-name)))
       (boolean)))

(defn open!
  [app]
  (match/match (shell/sh "open" "-a" app)
    {:exit 0} true))

(defn quit!
  [app]
  (match/match (shell/sh "oascript" "-e" (str "'quit app \"" app "\"'"))
    {:exit 0} true))

(comment

 (running? "Things3")
 (open! "Things3")
 (quit! "Things3")
 (running? "Deliveries")

 ,)
