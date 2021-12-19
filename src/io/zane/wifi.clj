(ns io.zane.wifi
  (:require [clojure.core.match :as match]
            [clojure.java.shell :as shell]))

(def ^:private airport
  "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport")

(defn connected?
  []
  (match/match (shell/sh "bash" "-c" (str airport " -I | grep \"state: running\""))
    {:exit 0} true
    :else false))

(def disconnected? (complement connected?))

(comment

 (connected?)
 (disconnected?)

 ,)
