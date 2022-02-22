(ns weather
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [io.zane.applescript :as applescript]
            [io.zane.bitbar :as bitbar]
            [io.zane.network :as network]
            [io.zane.wifi :as wifi]))

(def api-key "5b85b881806682e45c778588d5f27848")
(def mode "json")
(def units "imperial")

(def openweather-url "https://api.openweathermap.org/data/2.5/weather")

(defn round
  [n]
  (if (int? n)
    n
    (Math/round n)))

(def location-js
  (delay
    (.getPath (io/resource "location.js"))))

(defn location
  []
  (applescript/run-cljs
   '(-> (js/Application "Location Helper")
        (.getLocationCoordinates)
        (js->clj))))

(defn openweather
  []
  (let [[latitude longitude] (location)]
    (match/match (curl/get openweather-url
                           {:query-params {"lat" latitude
                                           "lon" longitude
                                           "appid" api-key
                                           "mode" mode
                                           "units" units}})
      {:status 200 :body body}
      (json/parse-string body true))))

(defn sfsymbol
  [id]
  ;; https://openweathermap.org/weather-conditions
  (case id
    "01d" "sun.max" ; clear sky
    "02d" "cloud.sun" ; few clouds
    "03d" "cloud" ; scattered clouds
    "04d" "cloud" ; broken clouds
    "09d" "cloud.heavyrain" ; shower rain
    "10d" "cloud.rain" ; rain
    "11d" "cloud.bolt.rain" ; thunderstorm
    "13d" "cloud.snow" ; snow
    "50d" "cloud.fog" ; mist

    "01n" "moon.stars" ; clear sky
    "02n" "cloud.moon" ; few clouds
    "03n" "cloud" ; scattered clouds
    "04n" "cloud" ; broken clouds
    "09n" "cloud.heavyrain" ; shower rain
    "10n" "cloud.rain" ; rain
    "11n" "cloud.bolt.rain" ; thunderstorm
    "13n" "cloud.snow" ; snow
    "50n" "cloud.fog")) ; fog

(defn temperature
  []
  (match/match (shell/sh "shortcuts" "run" "Temperature")
    {:exit 0 :out out} out))


(defn format-temp
  [n]
  (str (round n) "°"))

(defn -main
  []
  (if-not (network/up?)
    (do (println (bitbar/line "" {:sfimage (if (wifi/connected?)
                                             "wifi.exclamationmark"
                                             "wifi.slash")}))
        (println bitbar/separator)
        (println (bitbar/line "Refresh" {:sfimage "arrow.clockwise"
                                         :terminal false
                                         :refresh true})))
    (let [weather (openweather)
          temp (-> weather (get-in [:main :temp]) (format-temp))
          feels-like (-> weather (get-in [:main :feels_like]) (format-temp))
          icon  (-> weather (get-in [:weather 0 :icon]) (sfsymbol))
          min-temp (-> weather (get-in [:main :temp_min]) (format-temp))
          max-temp (-> weather (get-in [:main :temp_max]) (format-temp))]
      (println (bitbar/line temp {:sfimage icon}))
      (println bitbar/separator)
      (println (bitbar/line (str temp " (" min-temp " - " max-temp ")") {:sfimage "thermometer"}))
      (println (bitbar/line (str "Feels like " feels-like "°")))))
  (shutdown-agents))

(comment

  (openweather)

  ,)
