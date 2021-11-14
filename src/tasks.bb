(ns tasks
  (:import [java.time LocalDate]
           [java.time Instant]
           [java.time ZoneOffset]
           [java.util Date])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
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
       (when-let [due-date (:dueDate todo)]
         (nat-int? (compare (LocalDate/now) due-date)))))

(defn update-some
  [m k f]
  (cond-> m
    (contains? m k) (update k f)))

(defn local-date
  [s]
  (.toLocalDate
   (.atZone (Instant/parse s)
            ZoneOffset/UTC)))

(def things-js
  (delay
    (.getPath (io/resource "things.js"))))

(defn tasks
  []
  (let [parse-date (fn [s] (-> s (Instant/parse) (Date/from)))
        {:keys [err]} (applescript/run-js @things-js)]
    (map (fn [todo]
           (-> todo
               (update-some :dueDate local-date)
               (update-some :status keyword)))
         (json/parse-string err true))))

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
  (if-let [tasks (seq (tasks))]
    (when-let [first-task (some->> tasks
                                   (filter open?)
                                   (sort-by :due nils-last)
                                   (first))]
      (println (header-line first-task))
      (println bitbar/separator)
      (doseq [task tasks]
        (println (task-line task))))
    (print (bitbar/line "" {:sfimage sfimage}))))
