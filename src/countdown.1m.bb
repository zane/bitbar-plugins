#!/usr/local/bin/bb

(import '[java.time LocalDateTime])
(import '[java.time.temporal ChronoUnit])

(def date (LocalDateTime/of 2021 1 5 0 0))

(let [now (LocalDateTime/now)
      hours (.until now date ChronoUnit/HOURS)
      days (cond-> (.until now date ChronoUnit/DAYS)
             (> hours 0) (inc))]
  (cond (> days 0)  (println (str days " days"))
        (> hours 0) (println (str hours " hours"))))
