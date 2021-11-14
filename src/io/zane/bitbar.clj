(ns io.zane.bitbar
  (:require [clojure.string :as str]))

(def separator "---")

(defn param-str
  [m]
  (if (seq m)
    (str "|"
         (str/join \space (map #(str (name (key %)) "=" (val %))
                               m)))
    ""))

(defn line
  ([s]
   (line s {}))
  ([s m]
   (str s (param-str m))))
