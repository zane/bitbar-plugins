(ns calendar
  (:import [java.time ZonedDateTime]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit])
  (:require [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [io.zane.bitbar :as bitbar]
            [io.zane.http :as http]
            [io.zane.network :as network]
            [io.zane.string :as zane.string]
            [io.zane.time :as time]
            [io.zane.server :as server]
            [io.zane.url :as url]
            [io.zane.wifi :as wifi]))

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

(defn prompt-client
  [{:keys [port]}]
  (url/browse "https://accounts.google.com/o/oauth2/v2/auth"
              {:query-params {:client_id client-id
                              :redirect_uri (str "http://localhost:" port)
                              :response_type "code"
                              :scope "https://www.googleapis.com/auth/calendar.events.readonly"}}))

(defn oauth-code
  [opts]
  (let [code (future
               (let [{:keys [query-string]} (server/one-shot opts)]
                 (:code (url/query-string->map query-string))))]
    (prompt-client opts)
    @code))

(defn refresh-token
  [code {:keys [port]}]
  (let [{:keys [body]} (http/post token-uri
                                  {:form-params {"client_id" client-id
                                                 "client_secret" client-secret
                                                 "code" code
                                                 "grant_type" "authorization_code"
                                                 "redirect_uri" (str "http://localhost:" port)}})]
    (get (json/decode body) "refresh_token")))

(defn access-token
  [refresh-token]
  (match/match (http/post token-uri
                          {:form-params {"client_id" client-id
                                         "client_secret" client-secret
                                         "grant_type" "refresh_token"
                                         "refresh_token" refresh-token}})
    {:status 200 :body body} (:access_token (json/decode body true))
    {:status 400 :body body} (do (println (bitbar/line "" {:sfimage "calendar.badge.exclamationmark" :sfcolor "red"}))
                                 (println bitbar/separator)
                                 (println "Received status code 400")
                                 (println body)
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
        {:keys [body]} (http/get "https://www.googleapis.com/calendar/v3/calendars/primary/events"
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
  (let [formatter (DateTimeFormatter/ofPattern "h:mma")]
    (-> (.format date-time formatter)
        (str/lower-case))))

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
  (if-not (network/up?)
    (do (println (bitbar/line "" {:sfimage (if (wifi/connected?)
                                             "wifi.exclamationmark"
                                             "wifi.slash")}))
        (println bitbar/separator)
        (println (bitbar/line "Refresh" {:sfimage "arrow.clockwise"
                                         :terminal false
                                         :refresh true})))
    (let [{:keys [api-key max-minutes min-minutes] token :refresh-token :as state} (read-state)]
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
        (when (<= min-minutes minutes max-minutes)
          (println (bitbar/line (str (zane.string/truncate-words summary 25)
                                     (when-not (str/blank? time)
                                       (str " " time)))
                                {:sfimage "calendar"}))
          (println bitbar/separator)
          (println (when-let [urls (url-seq event)]
                     (->> urls
                          (map (fn [url]
                                 (bitbar/line (zane.string/truncate url 40)
                                              {:href url})))
                          (str/join "\n"))))
          (println bitbar/separator)
          (println (bitbar/line (str/join \space [summary "at" time-str]) {:sfimage "calendar.badge.clock"})))))))

(comment

  (-main)

  ,)
