(ns wanikani
  (:require [babashka.fs :as fs]
            [cheshire.core :as cheshire]
            [clojure.edn :as edn]
            [io.zane.bitbar :as bitbar]
            [org.httpkit.client :as client]))

(def api-token
  (delay
    (-> "~/.config/bitbar-plugins/wanikani.edn"
        (fs/expand-home)
        (fs/path)
        (str)
        (slurp)
        (edn/read-string)
        (get :bearer-token))))

(defn wanikani-get
  [url query-params]
  (let [{:keys [body status] :as res}
        (-> (client/get url {:headers {"Wanikani-Revision" 20170710
                                       "Authorization" (str "Bearer " @api-token)}
                             :query-params query-params})
            (deref))]
    (if-not (<= 200 status 299)
      (throw (ex-info "HTTP request failed" (select-keys res [:body :status])))
      (cheshire/decode body keyword))))

(defn lesson-count
  []
  (-> (wanikani-get "https://api.wanikani.com/v2/assignments"
                    {:immediately_available_for_lessons true})
      (:total_count)))

(defn review-count
  []
  (-> (wanikani-get "https://api.wanikani.com/v2/assignments"
                    {:immediately_available_for_review true})
      (:total_count)))

(defn -main
  []
  (let [lesson-count (lesson-count)
        review-count (review-count)]
    (when (or (pos? lesson-count)
              (pos? review-count))
      (println (bitbar/line "" {:sfimage "captions.bubble"}))
      (println bitbar/separator)
      (println (bitbar/line (str "WaniKani")))
      (println (bitbar/line (str "Lessons: " lesson-count) {:href "https://www.wanikani.com/lesson"}))
      (println (bitbar/line (str "Reviews: " review-count) {:href "https://www.wanikani.com/review"})))))
