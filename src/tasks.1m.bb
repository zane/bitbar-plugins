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

(defn complete?
  [todo]
  (boolean (:completion todo)))

(defn overdue?
  [todo]
  (when-let [due-date (:due todo)]
    (nat-int? (compare (LocalDate/now) due-date))))

(defn update-some
  [m k f]
  (cond-> m
    (contains? m k) (update k f)))

(defn local-date
  [s]
  (LocalDate/parse s))

(defn tasks
  []
  (let [{:keys [exit out] :as result} (shell/sh "osascript" "/Users/zane/Documents/BitBar/resources/things.scpt")]
    (if-not (zero? exit)
      (throw (ex-info "AppleScript returned a non-zero error code." result))
      (let [parse-dates (fn [m]
                          (reduce (fn [m k]
                                    (update-some m k local-date))
                                  m
                                  #{:due :completion}))]
        (into []
              (comp (map parse-dates))
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
(def ballot-box "☐")

(defn bitbar-task-header
  [task]
  (bitbar-line (:name task)
               (cond-> {}
                 (overdue? task) (assoc :color "red"))))

(defn bitbar-task-line
  [task]
  (str (bitbar-line (str (if (complete? task)
                           ballot-box-with-check
                           ballot-box)
                         " "
                         (:name task))
                    (if (complete? task)
                      {}
                      (cond-> {:href (show-url (:id task))}
                        (overdue? task) (assoc :color "red"))))))

(when-let [tasks (tasks)]
  (when-let [first-task (some->> tasks
                                 (remove complete?)
                                 (sort-by :due nils-last)
                                 (first))]
    (println (bitbar-task-header first-task))
    (println "---")
    (doseq [task tasks]
      (println (bitbar-task-line task)))))
