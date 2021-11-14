(ns zoom
  (:require [clojure.core.match :as match]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [io.zane.applescript :as applescript]
            [io.zane.bitbar :as bitbar]))

(defn zoom?
  "Returns `true` if Zoom is running."
  []
  (boolean
   (match/match (shell/sh "ps" "aux")
     {:exit 0 :out out} (re-find #"(?m)/zoom.us$" out))))

(def has-menu-js
  (delay
    (.getPath (io/resource "has-menu.js"))))

(defn has-menu?
  [app-name menu-bar-item menu-item]
  (if-not (zoom?)
    false
    (let [{:keys [err]} (applescript/run-js @has-menu-js app-name menu-bar-item menu-item)]
      (case (string/trim err)
        "true" true
        "false" false))))

(defn audio-on?
  []
  (has-menu? "zoom.us" "Meeting" "Mute Audio"))

(defn video-on?
  []
  (has-menu? "zoom.us" "Meeting" "Stop Video"))

(defn header-properties
  []
  (cond (video-on?) {:sfimage "video.bubble.left" :sfcolor "red"}
        (audio-on?) {:sfimage "phone.bubble.left" :sfcolor "red"}
        :else {:sfimage "bubble.left"}))

(defn -main
  []
  (when (zoom?)
    (println (bitbar/line "" (header-properties)))))
