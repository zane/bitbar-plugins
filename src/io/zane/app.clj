(ns io.zane.app
  (:require [io.zane.applescript :as applescript]))

(defn running?
  [app]
  (applescript/run-cljs `(~'.running (~'js/Application ~app))))

(defn open!
  [app]
  (applescript/run-cljs `(~'.launch (~'js/Application ~app))))

(defn quit!
  [app]
  (applescript/run-cljs `(~'.quit (~'js/Application ~app))))


(comment

  (running? "Things3")
  (open! "Things3")
  (quit! "Things3")

  ,)
