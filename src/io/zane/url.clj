(ns io.zane.url
  (:import [java.net URLDecoder URLEncoder])
  (:require [clojure.java.browse :as browse]
            [clojure.string :as string]))

(defn query-string->map
  [query-string]
  (->> (-> query-string
           (URLDecoder/decode)
           (string/split #"&"))
       (map (fn [s]
              (update (vec (string/split s #"="))
                      0
                      keyword)))
       (into {})))

(defn browse
  [url {:keys [query-params]}]
  (let [query-string (if (empty? query-params)
                       ""
                       (str "?" (string/join "&" (map #(str (URLEncoder/encode (name (key %)))
                                                            "="
                                                            (URLEncoder/encode (val %)))
                                                      query-params))))
        url (str url query-string)]
    (browse/browse-url url)))
