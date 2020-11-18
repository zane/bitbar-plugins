#!/usr/local/bin/bb

(require '[cheshire.core :as json])
(require '[clojure.java.shell :as shell])
(import '[java.time LocalDate])

(defn bitbar-line
  ([msg]
   (bitbar-line msg {}))
  ([msg opts]
   (str msg (when (seq opts)
              (str " | " (->> opts
                              (map #(str (name (key %)) "=" (val %)))
                              (interpose " ")
                              (apply str)))))))

(defn open?
  [todo]
  (= :open (:status todo)))

(defn complete?
  [todo]
  (= :completed (:status todo)))

(defn overdue?
  [todo]
  (and (= :open (:status todo))
       (when-let [due-date (:due todo)]
         (nat-int? (compare (LocalDate/now) due-date)))))

(defn update-some
  [m k f]
  (cond-> m
    (contains? m k) (update k f)))

(defn local-date
  [s]
  (LocalDate/parse s))

(defn things-applescript
  []
  (let [things-script "/Users/zane/projects/bitbar-plugins/resources/things.scpt"]
    (shell/sh "osascript" things-script)))

(defn tasks
  []
  (let [        {:keys [exit out] :as result} (things-applescript)]
    (if-not (zero? exit)
      (throw (ex-info "AppleScript returned a non-zero error code." result))
      (let [parse-dates (fn [m]
                          (reduce (fn [m k]
                                    (update-some m k local-date))
                                  m
                                  #{:due :completion}))]
        (into []
              (comp (map parse-dates)
                    (map #(update % :status keyword)))
              (json/parse-string out true))))))

(defn show-url
  [id]
  (str "things:///show?id=" id))

(defn nils-last
  [x y]
  (cond (and (nil? x) (nil? y)) 0
        (nil? x) 1
        (nil? y) -1
        :else (compare x y)))

(def ballot-box-with-check "☑")
(def ballot-box-with-x "☒")
(def ballot-box "☐")

(defn bitbar-task-header
  [task]
  (bitbar-line (:name task)
               (cond-> {}
                 (overdue? task) (assoc :color "red"))))

(defn bitbar-task-line
  [{:keys [status] :as task}]
  (str (bitbar-line (str (case status
                           :completed ballot-box-with-check
                           :canceled ballot-box-with-x
                           :open ballot-box)
                         " "
                         (:name task))
                    (assoc {:href (show-url (:id task))}
                           :color
                           (cond (overdue? task) "red"
                                 (open? task) "white"
                                 :else "#333333")))))

(when-let [tasks (tasks)]
  (when-let [first-task (some->> tasks
                                 (filter open?)
                                 (sort-by :due nils-last)
                                 (first))]
    (println (bitbar-task-header first-task))
    (println "---")
    (doseq [task tasks]
      (println (bitbar-task-line task)))))
