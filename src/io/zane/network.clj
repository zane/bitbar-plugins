(ns io.zane.network
  (:require [clojure.core.match :as match]
            [clojure.java.shell :as shell]))

(defn up?
  []
  (match/match (shell/sh "ping" "-o" "apple.com")
    {:exit 0} true
    :else false))

(def down? (complement up?))
