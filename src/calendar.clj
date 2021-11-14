(ns calendar
  (:import [java.net URLDecoder URLEncoder]
           [java.time ZonedDateTime]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit])
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.edn :as edn]
            [clojure.java.browse :as browse]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [io.zane.bitbar :as bitbar]
            [io.zane.string :as zane.string]
            [io.zane.time :as time]
            [org.httpkit.server :as server]))

(def state-file (io/as-relative-path "resources/state/calendar.edn"))

(defn read-state
  []
  (edn/read-string (slurp state-file)))

(defn write-state!
  [m]
  (spit state-file (with-out-str (pprint/pprint m))))

;; OAuth 2.0 client ID
(def json
  ;; https://console.cloud.google.com/apis/credentials?project=bitbar-plugins
  (-> (slurp "resources/state/client_id.json")
      (json/parse-string true)))

(def client-id (get-in json [:installed :client_id]))
(def client-secret (get-in json [:installed :client_secret]))
(def token-uri (get-in json [:installed :token_uri]))

(defn one-shot
  "Launches a web server that accepts one request, blocks waiting for a
  response, and then returns that response."
  [{:keys [port timeout] :or {port 8090 timeout 20000}}]
  (let [promise (promise)
        handler (fn [{:keys [query-string] :as request}]
                  (let [query-params (-> query-string )
                        request (assoc request :query-params query-params)]
                    (deliver promise request))
                  {:status 200
                   :body "Success! You can close this window now."})
        stop-server! (server/run-server handler {:port port :thread 1})
        result (deref promise timeout ::timeout)]
    (stop-server!)
    (case result
      ::timeout (throw (ex-info "timed out" {}))
      result)))

(defn browse-url
  [url {:keys [query-params]}]
  (let [query-string (if (empty? query-params)
                       ""
                       (str "?" (str/join "&" (map #(str (URLEncoder/encode (name (key %)))
                                                         "="
                                                         (URLEncoder/encode (val %)))
                                                   query-params))))
        url (str url query-string)]
    (browse/browse-url url)))

(defn prompt-client
  [{:keys [port]}]
  (browse-url "https://accounts.google.com/o/oauth2/v2/auth"
              {:query-params {:client_id client-id
                              :redirect_uri (str "http://localhost:" port)
                              :response_type "code"
                              :scope "https://www.googleapis.com/auth/calendar.events.readonly"}}))

(defn query-string->map
  [query-string]
  (->> (-> query-string
           (URLDecoder/decode)
           (str/split #"&"))
       (map (fn [s]
              (update (vec (str/split s #"="))
                      0
                      keyword)))
       (into {})))

(defn oauth-code
  [opts]
  (let [code (future
               (let [{:keys [query-string]} (one-shot opts)]
                 (:code (query-string->map query-string))))]
    (prompt-client opts)
    @code))

(defn refresh-token
  [code {:keys [port]}]
  (let [{:keys [body]} (curl/post token-uri
                                  {:form-params {"client_id" client-id
                                                 "client_secret" client-secret
                                                 "code" code
                                                 "grant_type" "authorization_code"
                                                 "redirect_uri" (str "http://localhost:" port)}})]
    (get (json/decode body) "refresh_token")))

(defn access-token
  [refresh-token]
  (match/match (curl/post token-uri
                          {:throw false
                           :form-params {"client_id" client-id
                                         "client_secret" client-secret
                                         "grant_type" "refresh_token"
                                         "refresh_token" refresh-token}})
    {:status 200 :body body} (:access_token (json/decode body true))
    {:status 400} (do (println (bitbar/line "" {:sfimage "calendar.badge.exclamationmark" :sfcolor "red"}))
                      (System/exit 0))))

(defn all-day?
  [event]
  (some? (get-in event [:start :date])))

(defn declined?
  [event email]
  (some->> (get event :attendees)
           (filter #(= email (:email %)))
           (first)
           (:responseStatus)
           (= "declined")))

(defn parse-event
  [event]
  (let [parse #(ZonedDateTime/parse % (DateTimeFormatter/ISO_DATE_TIME))]
    (update-in event [:start :dateTime] parse)))

(defn next-event
  [api-key access-token {:keys [email]}]
  (let [now (.format
             (ZonedDateTime/now)
             (DateTimeFormatter/ISO_INSTANT))
        {:keys [body]} (curl/get "https://www.googleapis.com/calendar/v3/calendars/primary/events"
                                 {:headers {"Authorization" (str "Bearer " access-token)}
                                  :query-params {"key" api-key
                                                 "maxResults" "10"
                                                 "orderBy" "startTime"
                                                 "singleEvents" "True"
                                                 "timeMin" now}})
        {:keys [items]} (json/parse-string body true)]
    (when-let [event (->> items
                          (remove all-day?)
                          (remove #(declined? % email))
                          (first)
                          (parse-event))]
      event)))

(defn time-str
  [date-time]
  (-> date-time
      (.format (DateTimeFormatter/ofPattern "h:mma"))
      (str/lower-case)))

(defn minutes-until
  [date-time]
  (.between ChronoUnit/MINUTES (ZonedDateTime/now) date-time))

(defn url-seq
  [event]
  (mapcat #(when-let [s (get event %)]
             (re-seq #"https?://[^\s<>]+" s))
          [:location :description]))

(defn -main
  [& _]
  (let [{:keys [api-key max-minutes] token :refresh-token :as state} (read-state)]
    (when-not token
      (let [oauth-code (oauth-code state)
            token (refresh-token oauth-code state)]
        (write-state! (assoc state :refresh-token token))))
    (let [{:keys [refresh-token]} (read-state)
          token (access-token refresh-token)
          event (next-event api-key token state)
          summary (get-in event [:summary])
          start-time (get-in event [:start :dateTime])
          time (time/to start-time)
          minutes (minutes-until start-time)
          time-str (time-str start-time)]
      (if-not (<= minutes max-minutes)
        (println (bitbar/line "" {:sfimage "calendar"}))
        (do (println (bitbar/line (str (zane.string/truncate-words summary 25) " in " time) {:sfimage "calendar"}))
            (println bitbar/separator)
            (println (when-let [urls (url-seq event)]
                       (->> urls
                            (map (fn [url]
                                   (bitbar/line (zane.string/truncate url 40)
                                                {:href url})))
                            (str/join "\n"))))))
      (println bitbar/separator)
      (println (bitbar/line (str/join \space [summary "at" time-str]) {:sfimage "calendar.badge.clock"})))))

(comment

 (-main)

 ,)
