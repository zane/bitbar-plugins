(ns tasks
  (:import [java.time LocalDate]
           [java.time ZoneOffset])
  (:require [io.zane.app :as app]
            [io.zane.applescript :as applescript]
            [io.zane.bitbar :as bitbar]
            [io.zane.string :as zane.string]))

(defn open?
  [todo]
  (= :open (:status todo)))

(defn complete?
  [todo]
  (= :completed (:status todo)))

(defn overdue?
  [todo]
  (and (= :open (:status todo))
       (when-let [due-date (:due-date todo)]
         (nat-int? (compare (LocalDate/now) due-date)))))

(defn update-some
  [m k f]
  (cond-> m
    (contains? m k) (update k f)))

(defn local-date
  [^java.util.Date d]
  (.toLocalDate
   (.atZone (.toInstant d)
            ZoneOffset/UTC)))

(defn tasks
  []
  (->> (applescript/run-cljs
        '(for [todo (-> (js/Application "Things") (.-lists) (.byName "Today") (.toDos))]
           (->> {:id (.id todo)
                 :name (.name todo)
                 :due-date (.dueDate todo)
                 :status (.status todo)}
                (filter (comp some? val))
                (into {}))))
       (map #(-> %
                 (update-some :due-date local-date)
                 (update-some :status keyword)))))

(defn show-url
  [id]
  (str "things:///show?id=" id))

(defn nils-last
  "Comparator. Returns a negative number, 0, or a positive number when x is
  logically 'less than', 'equal to', or 'greater than' y. Same as `compare`
  except it also works for nil. nils are sorted to the bottom."
  [x y]
  (cond (and (nil? x) (nil? y)) 0
        (nil? x) 1
        (nil? y) -1
        :else (compare x y)))

(def sfimage "checklist")

(defn header-line
  [task]
  (bitbar/line (zane.string/truncate-words (:name task)
                                           25)
               {:sfimage sfimage}))

(defn task-line
  [{:keys [status] :as task}]
  (let [sfimage (case status
                  :completed "checkmark.square"
                  :canceled "x.square"
                  :open "square")
        show-url (show-url (:id task))]
    (bitbar/line (:name task) {:sfimage sfimage :href show-url})))

(defn -main
  []
  (if-not (app/running? "Things3")
    (do (println (bitbar/line "" {:sfimage "questionmark.app"}))
        (println bitbar/separator)
        (println (bitbar/line "Launch Things" {:bash "open"
                                               :param0 "/Applications/Things3.app"
                                               :terminal false
                                               :refresh true})))
    (when-let [tasks (seq (tasks))]
      (when-let [first-task (some->> (seq tasks)
                                     (filter open?)
                                     (sort-by :due nils-last)
                                     (first))]
        (println (header-line first-task))
        (println bitbar/separator)
        (doseq [task tasks]
          (println (task-line task))))))
  (System/exit 0))
