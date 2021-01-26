#!/usr/local/bin/bb

(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])
(import '[java.time LocalDate])
(import '[java.time.format DateTimeFormatter])

(def api-key "d79ce9969d3740b4a58e774d1f7cadcd")
(def nyc-fips "36061")
(def brooklyn-fips "36047")

(defn url
  [api-key fips]
  (str "https://api.covidactnow.org/v2/county/" fips ".timeseries.json?apiKey=" api-key))

(defn format-percent
  [n]
  (str (format "%.1f" (* 100 n))
       "%"))

(let [{:keys [body status] :as response} (curl/get (url api-key brooklyn-fips)
                                                   {:throw false})]
  (if-not (= 200 status)
    (throw (ex-info "API returned bad status code:" response))
    (let [json (json/parse-string body true)]
      (println (format-percent (get-in json [:metrics :testPositivityRatio])))
      (println "---")
      (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")
            last-updated (LocalDate/parse (:lastUpdatedDate json))
            percentages (->> (iterate #(.minusDays % 7) last-updated)
                             (keep (fn [local-date]
                                     (some #(when (= (.format local-date formatter)
                                                     (:date %))
                                              (:testPositivityRatio %))
                                           (:metricsTimeseries json))))
                             (partition 2)
                             (take 7)
                             (map #(- (first %) (second %))))]
        (doseq [percentage percentages]
          (println (format-percent percentage)))))))
