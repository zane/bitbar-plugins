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

(defn single-cbsa-timeseries
  [cbsa api-key]
  ;; https://apidocs.covidactnow.org/api#tag/CBSA-Data/paths/~1cbsa~1{cbsa_code}.timeseries.json?apiKey={apiKey}/get
  (let [url (str   "https://api.covidactnow.org/v2/cbsa/" cbsa ".timeseries.json?apiKey=" api-key)]
    (match/match (curl/get url {:throw false})
      {:status 200 :body body}
      (json/parse-string body true))))

(defn overall-risk-level
  [m]
  (get-in m [:riskLevels :overall]))

(defn format-percent
  [n]
  (str (format "%.1f" (* 100 n))
       "%"))

(defn metric-type
  [k]
  (case k
    :caseDensity :float
    :infectionRate :float
    :testPositivityRatio :percent
    :string))

(defn type-formatter
  [k]
  (case k
    :percent format-percent
    :float str
    :string str))

(defn format-risk-level
  [n]
  (case n
    0 "Low"
    1 "Medium"
    2 "High"
    3 "Very high"
    4 "Unknown"
    5 "Extremely high"))

(defn risk-color
  [n]
  ;; https://covidactnow.org/covid-risk-levels-metrics
  (case n
    0 "#00d474" ; "green"
    1 "#ffc900" ; "yellow"
    2 "#ff9600" ; "orange"
    3 "#d9002c" ; "red"
    4 "black"
    5 "#790019")) ; "maroon"

(defn metric-name
  [k]
  (case k
    :caseDensity "Daily new cases"
    :infectionRate "Infection rate"
    :testPositivityRatio "Positive test rate"))

(defn metric-line
  [k summary url]
  (let [metric-name (metric-name k)
        format (-> (metric-type k)
                   (type-formatter))
        value (get-in summary [:metrics k])
        risk-color (-> (get-in summary [:riskLevels k])
                       (risk-color))]
    (bitbar/line (str ":circle.fill: " metric-name ": " (format value))
                 {:sfcolor risk-color
                  :href url})))

(defn -main
  [& _]
  (when (network/up?)
    (let [summary (single-cbsa-summary (get cbsa :ny-nj-pa) @api-key)
          overall-risk-level (overall-risk-level summary)
          overall-risk-color (risk-color overall-risk-level)
          risk (format-risk-level overall-risk-level)
          url "https://covidactnow.org/us/metro/new-york-city-newark-jersey-city_ny-nj-pa/?s=28841761"]
      (println (bitbar/line "" {:sfimage "facemask"}))
      (println bitbar/separator)
      (println (bitbar/line (str ":circle.fill: Risk level: " risk) {:href url :sfcolor overall-risk-color}))
      (println bitbar/separator)
      (doseq [line (map #(metric-line % summary url)
                        [:caseDensity :infectionRate :testPositivityRatio])]
        (println line)))))

(comment

  (set! *print-length* 10)

  (-main)

  (single-county-summary (:kings fips) @api-key)
  (single-cbsa-summary (:ny-nj-pa cbsa) @api-key)
  (single-cbsa-timeseries (:ny-nj-pa cbsa) @api-key)

  (keys (single-cbsa-timeseries (:ny-nj-pa cbsa) @api-key))

  (let [summary (single-cbsa-summary (:ny-nj-pa cbsa) @api-key)]
    (map #(metric-line % summary) [:caseDensity :infectionRate :testPositivityRatio]))

  (single-cbsa-summary (:ny-nj-pa cbsa) @api-key)

  ,)
