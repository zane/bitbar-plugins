(ns weather
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.java.shell :as shell]
            [io.zane.bitbar :as bitbar]))

(def api-key "5b85b881806682e45c778588d5f27848")
(def mode "json")
(def units "imperial")

(def openweather-url "https://api.openweathermap.org/data/2.5/weather")

(defn round
  [n]
  (if (int? n)
    n
    (Math/round n)))

(defn location
  []
  (match/match (shell/sh "shortcuts" "run" "Current location")
    {:exit 0 :out out} (json/parse-string out true)))

(defn openweather
  []
  (let [{:keys [latitude longitude]} (location)]
    (curl/get openweather-url
              {:query-params {"lat" latitude
                              "lon" longitude
                              "appid" api-key
                              "mode" mode
                              "units" units}})))

(defn sfsymbol
  [id]
  ;; https://openweathermap.org/weather-conditions
  (case id
    "01d" "sun.max"
    "02d" "cloud.sun"
    "03d" "cloud"
    "04d" "cloud"
    "09d" "cloud.sun.rain"
    "10d" "cloud.sun.rain"
    "11d" "cloud.sun.bolt"
    "13d" "cloud.snow"
    "50d" "cloud.fog"

    "01n" "moon.stars"
    "02n" "cloud.moon"
    "03n" "cloud"
    "04n" "cloud"
    "09n" "cloud.moon.rain"
    "10n" "cloud.moon.rain"
    "11n" "cloud.moon.bolt"
    "13n" "cloud.snow"
    "50n" "cloud.fog"))

(defn -main
  []
  (let [weather (-> (openweather)
                    (:body)
                    (json/parse-string true))
        temp (-> weather (get-in [:main :feels_like]) (round))
        feels-like (-> weather (get-in [:main :feels_like]) (round))
        icon  (-> weather (get-in [:weather 0 :icon]) (sfsymbol))
        min-temp (-> weather (get-in [:main :temp_min]) (round))
        max-temp (-> weather (get-in [:main :temp_max]) (round))]
    (println (bitbar/line (str feels-like "°") {:sfimage icon}))
    (println bitbar/separator)
    (println (bitbar/line (str temp "° (" min-temp "-" max-temp "°)") {:sfimage "thermometer"}))))
