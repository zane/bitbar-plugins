#!/usr/local/bin/bb

(ns countdown
  (:require [io.zane.bitbar :as bitbar]
            [io.zane.time :as time])
  (:import [java.time.format DateTimeFormatter]))

(def date (time/local-date 2022 4 10))

(defn -main
  [& _]
  (let [date-s (.format (DateTimeFormatter/ofPattern "MMMM d, YYYY")
                        date)]
    (println (bitbar/line (time/to date) {:sfimage ":hourglass:"}))
    (println bitbar/separator)
    (println (bitbar/line (str "until " date-s)))))

(comment

 (-main)

 ,)
