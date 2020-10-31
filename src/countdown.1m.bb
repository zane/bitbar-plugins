#!/usr/local/bin/bb

(import '[java.time LocalDate])
(import '[java.time ZoneId])

(def election-day (LocalDate/of 2020 11 3))

(def est (ZoneId/of "America/New_York"))

(let [days (.. (LocalDate/now est)
               (until election-day)
               (getDays))]
  (println (str days " days")))
