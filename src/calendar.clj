(ns calendar
  (:import [java.time ZonedDateTime]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]
           [java.time.temporal TemporalAccessor])
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as string]
            [io.zane.bitbar :as bitbar]
            [io.zane.string :as zane.string]
            [io.zane.time :as time]))

(defn parse-iso-8601
  [s]
  (when-not (or (nil? s)
                (string/blank? s))
    (-> ^TemporalAccessor (.parse DateTimeFormatter/ISO_DATE_TIME s)
        (ZonedDateTime/from))))

(defn parse-yn
  [s]
  (when s
    (case s
      "Yes" true
      "No" false)))

(defn next-event
  []
  (let [json (-> (process/shell {:out :string} "shortcuts run 'Next event'")
                 (:out))]
    (when-not (string/blank? json)
      (-> json
          (json/decode keyword)
          (update :start parse-iso-8601)
          (update :end parse-iso-8601)
          (update :all-day parse-yn)))))

(defn next-reminder
  []
  (let [json (-> (process/shell {:out :string} "shortcuts run 'Next reminder'")
                 (:out))]
    (when-not (string/blank? json)
      (-> json
          (json/decode keyword)
          (update :start parse-iso-8601)))))

(defn next-item
  []
  (let [epoch-millis #(-> % (.toInstant) (.toEpochMilli))
        event (next-event)
        reminder (next-reminder)]
    (if-not (and event reminder)
      (or event reminder)
      (min-key (comp epoch-millis :start) event reminder))))


(comment

  (next-event)
  (next-reminder)
  (next-item)

  ,)

(defn time-str
  [date-time]
  (let [formatter (DateTimeFormatter/ofPattern "h:mma")]
    (-> (.format date-time formatter)
        (string/lower-case))))

(defn minutes-until
  [date-time]
  (.between ChronoUnit/MINUTES (ZonedDateTime/now) date-time))

(defn url-seq
  [coll]
  (mapcat #(when (string? %)
            (re-seq #"https?://[^\s<>,]+" %))
          (tree-seq coll? seq coll)))

(defn -main
  [& _]
  (let [item (next-item)]
    (when item
      (let [{:keys [start title]} item
            time (time/to start)
            time-str (time-str start)]
        (when (<= 0 (minutes-until start) 60)
          (println (bitbar/line (str (zane.string/truncate-words title 25)
                                     (when-not (string/blank? time)
                                       (str " " time)))
                                {:sfimage "calendar"}))
          (println bitbar/separator)
          (println (when-let [urls (url-seq item)]
                     (->> urls
                          (map (fn [url]
                                 (bitbar/line (zane.string/truncate url 40)
                                              {:href url})))
                          (string/join "\n"))))
          (println bitbar/separator)
          (println (bitbar/line (string/join \space [title "at" time-str]) {:sfimage "calendar.badge.clock"})))))))

(comment

  (url-seq (next-event))
  (-main)

  ,)
