(ns io.zane.applescript
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]))

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

(defn run-js-str
  [s & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" "-l" "JavaScript" :in s rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))

(defn run-cljs
  [form & rest]
  (let [obb "/Users/zane/projects/obb/out/bin/obb"
        {:keys [exit out] :as result} (apply shell/sh obb "-e" (pr-str form) rest)]
    (if (zero? exit)
      (edn/read-string out)
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))

(comment

  (set! *print-length* 10)

  ,)
