(ns io.zane.wifi
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [clojure.java.shell :as shell]))

(defn airport-info
  []
  (let [parse-int #(Integer/parseInt %)
        parsers {:lastTxRate parse-int
                 :guardInterval parse-int
                 :agrExtRSSI parse-int
                 :agrCtlRSSI parse-int
                 :agrCtlNoise parse-int
                 :NSS parse-int
                 :MCS parse-int
                 :agrExtNoise parse-int
                 :lastAssocStatus parse-int
                 :maxRate parse-int}
        rf (fn [acc s]
             (if-let [[_ k v] (re-find #"^[\s]*(.*):[\s]*(.*[^\s])[\s]*" s)]
               (if (string/blank? v)
                 acc
                 (let [k (keyword (string/replace k #"[\s]" "-"))
                       v (if-let [f (get parsers k)]
                           (f v)
                           v)]
                   (assoc acc k v)))
               acc))]
    (match/match (shell/sh "airport" "-I")
      {:exit 0 :out out}
      (reduce rf {} (string/split-lines out)))))

(defn connected?
  []
  (let [{:keys [state]} (airport-info)]
    (= "running" state)))

(def disconnected? (complement connected?))

(comment

 (connected?)
 (disconnected?)

 ,)
