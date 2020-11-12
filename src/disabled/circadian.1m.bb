#!/usr/local/bin/bb

(require '[clojure.edn :as edn])
(require '[clojure.java.shell :as shell])
(require '[clojure.string :as string])

(import '[java.time ZonedDateTime])
(import '[java.time ZoneId])
(import '[java.time.format DateTimeFormatter])

(defn interface-style
  []
  (let [{:keys [exit]} (shell/sh "defaults" "read" "-g" "AppleInterfaceStyle")]
    (case exit
      1 :light
      0 :dark)))

(defn sun-schedule
  []
  (let [formatter (DateTimeFormatter/ofPattern "uuuu-MM-dd HH:mm:ss Z")
        zone-id (ZoneId/systemDefault)
        parse-date #(-> (ZonedDateTime/parse % formatter)
                        (.withZoneSameInstant zone-id))
        {:keys [exit out] :as result} (shell/sh "/usr/libexec/corebrightnessdiag" "sunschedule")]
    (if-not (zero? exit)
      (throw (ex-info "Could not execute corebrightnessdiag." result))
      (-> (into {}
                (comp (keep #(re-matches #"^\s+([^\s]+)\s*=\s*([^\s].*);$" %))
                      (map #(vec (drop 1 %)))
                      (map #(update % 0 keyword))
                      (map #(update % 1 edn/read-string)))
                (string/split-lines out))
          (update :isDaylight #(case % 0 true 1 false))
          (update :sunrise parse-date)
          (update :sunset parse-date)
          (update :nextSunrise parse-date)
          (update :nextSunset parse-date)
          (update :previousSunrise parse-date)
          (update :previousSunset parse-date)))))

#_(interface-style)
#_(sun-schedule)
