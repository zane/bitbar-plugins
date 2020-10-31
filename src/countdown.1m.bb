#!/usr/local/bin/bb

(import '[java.time LocalDateTime])
(import '[java.time ZoneId])
(import '[java.time.temporal ChronoUnit])

(def date (LocalDateTime/of 2020 11 3 0 0))

(def est (ZoneId/of "America/New_York"))

(let [now (LocalDateTime/now est)
      hours (.until now date ChronoUnit/HOURS)
      days (cond-> (.until now date ChronoUnit/DAYS)
             (> hours 0) (inc))]
  (cond (> days 0)  (println (str days " days"))
        (> hours 0) (println (str hours " hours"))))
