#!/usr/local/bin/bb

(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])

(def url "https://static01.nyt.com/elections-assets/2020/data/api/2020-11-03/votes-remaining-page/national/president.json")

(def pa-index 38)

(let [{:keys [body]}
      (curl/get url)

      all-candidates
      (get-in (json/parse-string body true)
              [:data :races pa-index :candidates])

      {[biden] "Biden" [trump] "Trump"}
      (group-by :last_name all-candidates)

      total (transduce (map :votes) + all-candidates)]
  (->> (/ (- (:votes biden)
             (:votes trump))
          total)
       (* 100)
       (float)
       (printf "%.2f%%")))
