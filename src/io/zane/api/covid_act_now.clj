(ns io.zane.api.covid-act-now
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [io.zane.bitbar :as bitbar]
            [io.zane.network :as network]
            [io.zane.password :as password]))

(def cbsa
  ;; https://www.census.gov/geographies/reference-files/time-series/demo/metro-micro/delineation-files.html
  {:ny-nj-pa "35620"
   :sf-bay-area "41860"})

(def fips
  ;; https://en.wikipedia.org/wiki/List_of_counties_in_New_York
  {:bronx "36005"
   :kings "36047"
   :queens "36059"
   :new-york "36061"
   :richmond "36085"})

(def api-key (delay (password/find-generic "Covid Act Now API" "zaneshelby@gmail.com")))

(defn single-county-summary
  [fips api-key]
  ;; https://apidocs.covidactnow.org/api#tag/County-Data/paths/~1county~1{fips}.json?apiKey={apiKey}/get
  (let [url (str "https://api.covidactnow.org/v2/county/" fips ".json?apiKey=" api-key)]
    (match/match (curl/get url {:throw false})
      {:status 200 :body body}
      (json/parse-string body true))))

(defn single-county-timeseries
  [fips api-key]
  ;; https://apidocs.covidactnow.org/api#tag/County-Data/paths/~1county~1{fips}.timeseries.json?apiKey={apiKey}/get
  (let [url (str "https://api.covidactnow.org/v2/county/" fips ".timeseries.json?apiKey=" api-key)]
    url
    (match/match (curl/get url {:throw false})
      {:status 200 :body body}
      (json/parse-string body true))))

(defn single-cbsa-summary
  [cbsa api-key]
  ;; https://apidocs.covidactnow.org/api#tag/CBSA-Data/paths/~1cbsa~1{cbsa_code}.json?apiKey={apiKey}/get
  (let [url (str "https://api.covidactnow.org/v2/cbsa/" cbsa ".json?apiKey=" api-key)]
    url
    (match/match (curl/get url {:throw false})
      {:status 200 :body body}
      (json/parse-string body true))))

(defn test-positivity-rate
  [m]
  (get-in m [:metrics :testPositivityRatio]))

(defn overall-risk-level
  [m]
  (get-in m [:riskLevels :overall]))

(defn format-percent
  [n]
  (str (format "%.1f" (* 100 n))
       "%"))

(defn format-risk-level
  [n]
  (case n
    0 "Low"
    1 "Medium"
    2 "High"
    3 "Very high"
    4 "Unknown"
    5 "Extremely high"))

(defn -main
  [& _]
  (when (network/up?)
    (let [summary (single-cbsa-summary (get cbsa :ny-nj-pa) @api-key)
          pos-rate (-> (test-positivity-rate summary)
                       (format-percent))
          risk (-> (overall-risk-level summary)
                   (format-risk-level))]
      (println (bitbar/line "" {:sfimage "facemask"}))
      (println bitbar/separator)
      (println (bitbar/line (str "Risk level: " risk)))
      (println (bitbar/line (str "Positivity rate: " pos-rate))))))

(comment

  (set! *print-length* 10)

  (-main)

  (format-percent (test-positivity-rate (single-county-summary (:kings fips) @api-key)))
  (format-percent (test-positivity-rate (single-cbsa-summary (:ny-nj-pa cbsa) @api-key)))
  (format-percent (test-positivity-rate (single-cbsa-summary (:sf-bay-area cbsa) @api-key)))

  ,)
