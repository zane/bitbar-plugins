#!/usr/local/bin/bb

(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])

;; https://github.com/chubin/wttr.in
;; https://formulae.brew.sh/formula/darksky-weather

(defn bitbar-line
  ([msg]
   (bitbar-line msg {}))
  ([msg opts]
   (str msg (when (seq opts)
              (str " | " (->> opts
                              (map #(str (name (key %)) "=" (val %)))
                              (interpose " ")
                              (apply str)))))))
(defn safe-curl-get
  "Issues a HTTP request using `curl/get` and returns the response. Throws if the
  status code of the response is not between 200 and 299."
  ([url]
   (safe-curl-get url {}))
  ([url opts]
   (let [{:keys [status] :as response} (curl/get url opts)]
     (if (<= 200 status 299)
       response
       (throw (ex-info (str "HTTP status code " status)
                       response))))))

(def zip "11206")

(def wttr-url (str "http://wttr.in/" zip "?u"))
(def darksky-url (str "https://darksky.net/forecast/" zip))

(let [weather (-> (safe-curl-get (str wttr-url "&format=j1"))
                  (:body)
                  (json/parse-string true))
      feels-like (get-in weather [:current_condition 0 :FeelsLikeF])
      min-temp (get-in weather [:weather 0 :mintempF])
      max-temp (get-in weather [:weather 0 :maxtempF])]
  (println (str feels-like "°"))
  (println "---")
  (println (bitbar-line (str min-temp "-" max-temp "°")
                        {:href wttr-url})))
