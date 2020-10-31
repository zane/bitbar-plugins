#!/usr/local/bin/bb

(import '[java.time LocalDateTime])
(import '[java.time ZoneId])
(import '[java.time.temporal ChronoUnit])

(def date (LocalDateTime/of 2020 11 1 0 0))

(def est (ZoneId/of "America/New_York"))

(let [days (.. (LocalDateTime/now est)
               (until date ChronoUnit/DAYS))]
  (if (> days 0)
    (println (str days " days"))
    (let [hours (.. (LocalDateTime/now est)
                    (until date ChronoUnit/HOURS))]
      (when (> hours 0)
        (println (str hours " hours"))))))
