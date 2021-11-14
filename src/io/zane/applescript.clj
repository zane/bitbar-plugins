(ns io.zane.applescript
  (:require [clojure.java.shell :as shell]))

(defn run
  [path & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" path rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))


(defn run-js
  [path & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" "-l" "JavaScript" path rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))
