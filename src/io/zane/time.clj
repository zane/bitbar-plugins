(ns io.zane.time
  (:import [java.time Duration]
           [java.time LocalDate]
           [java.time Period]
           [java.time ZonedDateTime]
           [java.time ZoneId])
  (:require [clojure.string :as string]))

(defn local-date
  [year month day]
  (.atStartOfDay (LocalDate/of year month day)
                 (ZoneId/systemDefault)))

(defn format-period
  [period]
  (->> [{:unit {:plural "years" :singular "year"} :amount (.getYears period)}
        {:unit {:plural "months" :singular "month"} :amount (.getMonths period)}
        {:unit {:plural "days" :singular "day"} :amount (.getDays period)}]
       (filter (comp pos? :amount))
       (map (fn [{:keys [amount] {:keys [singular plural]} :unit}]
              (when (pos? amount)
                (let [unit (if (= 1 amount)
                             singular
                             plural)]
                  (str amount \space  unit)))))
       (string/join \space)))

(defn format-duration
  [duration]
  (let [hours (.toHoursPart duration)
        minutes (.toMinutesPart duration)
        seconds (.toSecondsPart duration)]
    (str (when (pos? hours)
           (str hours "h"))
         (when (and (zero? hours)
                    (pos? minutes))
           (str minutes "m"))
         (when (and (zero? hours)
                    (zero? minutes))
           (str seconds "s")))))

(defn to
  [zdt]
  (let [now (ZonedDateTime/now)
        period (Period/between (.toLocalDate now) (.toLocalDate zdt))
        duration (Duration/between now zdt)]
    (str (when-not (or (.isNegative period)
                       (.isNegative duration))
           "in ")
         (if-not (= Period/ZERO period)
           (format-period period)
           (format-duration duration)))))


(comment

 (to (-> (ZonedDateTime/now)
         (.plusHours 1)
         (.plusMinutes 50)))

 (to (-> (ZonedDateTime/now)
         (.minusHours 1)
         (.minusMinutes 50)))


 ,)
