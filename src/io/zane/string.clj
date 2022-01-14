(ns io.zane.string
  (:require [clojure.string :as string]))

(defn truncate
  [s n]
  (if-not (> (count s) n)
    s
    (str (subs s 0 n) "…")))

(defn conjunction?
  [s]
  (contains? #{"about"
               "and"
               "at"
               "but"
               "for"
               "in"
               "nor"
               "or"
               "so"
               "the"
               "yet"}
             (string/lower-case s)))

(defn last-word
  [s]
  (last (string/split s #"\s")))

(defn truncate-words
  [s n]
  (if-not (> (count s) n)
    s
    (str (->> (string/split s #"\s")
              (reductions #(string/join " " %&))
              (take-while #(<= (count %) (dec n)))
              (remove (comp conjunction? last-word))
              (last))
          "…")))
