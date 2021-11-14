(ns io.zane.http
  (:refer-clojure :exclude [get])
  (:import [java.net URI]
           [java.net URLEncoder]
           [java.net.http HttpClient]
           [java.net.http HttpClient$Redirect]
           [java.net.http HttpClient$Version]
           [java.net.http HttpRequest]
           [java.net.http HttpRequest$BodyPublishers]
           [java.net.http HttpRequest$Builder]
           [java.net.http HttpResponse$BodyHandlers])
  (:require [clojure.string :as string]))

(defn client
  []
  (-> (HttpClient/newBuilder)
      (.version HttpClient$Version/HTTP_1_1)
      (.followRedirects HttpClient$Redirect/ALWAYS)
      (.build)))

(defprotocol UrlEncode
  (url-encode [x]))

(extend-protocol UrlEncode
  String
  (url-encode [s]
    (URLEncoder/encode s))

  clojure.lang.Keyword
  (url-encode [k]
    (url-encode (name k)))

  clojure.lang.PersistentArrayMap
  (url-encode [m]
    (if-not (seq m)
      ""
      (->> m
           (map #(str (url-encode (key %))
                      "="
                      (url-encode (val %))))
           (string/join "&"))))

  java.lang.Long
  (url-encode [n]
    (url-encode (str n))))

(defn ^:private string-publisher
  [s]
  (HttpRequest$BodyPublishers/ofString s))

(defn response->map
  [response]
  {:body (.body response)
   :status (.statusCode response)})

(defn ^:private add-headers!
  [^HttpRequest$Builder builder headers]
  (reduce-kv (fn [builder k v] (.header builder (name k) (str v)))
             builder
             headers))

(defn get
  [uri {:keys [headers query-params]}]
  (let [uri (cond-> uri (seq query-params) (str "?" (url-encode query-params)))
        builder (cond-> (-> (HttpRequest/newBuilder)
                            (.uri (URI/create uri)))
                  (seq headers) (add-headers! headers))
        request (.build builder)
        response (.send (client) request (HttpResponse$BodyHandlers/ofString))]
    (response->map response)))

(defn post
  [uri {:keys [headers form-params]}]
  (let [headers (cond-> headers
                  (seq form-params)
                  (assoc :content-type "application/x-www-form-urlencoded"))
        builder (cond-> (-> (HttpRequest/newBuilder)
                            (.uri (URI/create uri))
                            (.POST (string-publisher (url-encode form-params))))
                  (seq headers)
                  (add-headers! headers))
        request (.build builder)
        response (.send (client) request (HttpResponse$BodyHandlers/ofString))]
    (response->map response)))

(comment

 (get "http://www.google.com" {:query-params {:a "b"}})
 (post "https://ptsv2.com/t/9cdwy-1636861612/post" {:form-params {:a "b" :c "d"}})

 ,)
