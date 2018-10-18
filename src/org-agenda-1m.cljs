#!/usr/local/bin/planck

(require '[clojure.string :as string])
(require '[goog.labs.format.csv :as csv])
(require '[goog.string.format :as format])
(require '[planck.shell :as shell])
(require '[planck.from.io.aviso.ansi :as ansi])

(def icon "iVBORw0KGgoAAAANSUhEUgAAACQAAAAkCAYAAADhAJiYAAAAAXNSR0IArs4c6QAAAAlwSFlzAAAWJQAAFiUBSVIk8AAABCRpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IlhNUCBDb3JlIDUuNC4wIj4KICAgPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICAgICAgPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIKICAgICAgICAgICAgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iCiAgICAgICAgICAgIHhtbG5zOmV4aWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vZXhpZi8xLjAvIgogICAgICAgICAgICB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iCiAgICAgICAgICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyI+CiAgICAgICAgIDx0aWZmOlJlc29sdXRpb25Vbml0PjI8L3RpZmY6UmVzb2x1dGlvblVuaXQ+CiAgICAgICAgIDx0aWZmOkNvbXByZXNzaW9uPjU8L3RpZmY6Q29tcHJlc3Npb24+CiAgICAgICAgIDx0aWZmOlhSZXNvbHV0aW9uPjE0NDwvdGlmZjpYUmVzb2x1dGlvbj4KICAgICAgICAgPHRpZmY6T3JpZW50YXRpb24+MTwvdGlmZjpPcmllbnRhdGlvbj4KICAgICAgICAgPHRpZmY6WVJlc29sdXRpb24+MTQ0PC90aWZmOllSZXNvbHV0aW9uPgogICAgICAgICA8ZXhpZjpQaXhlbFhEaW1lbnNpb24+MzY8L2V4aWY6UGl4ZWxYRGltZW5zaW9uPgogICAgICAgICA8ZXhpZjpDb2xvclNwYWNlPjE8L2V4aWY6Q29sb3JTcGFjZT4KICAgICAgICAgPGV4aWY6UGl4ZWxZRGltZW5zaW9uPjM2PC9leGlmOlBpeGVsWURpbWVuc2lvbj4KICAgICAgICAgPGRjOnN1YmplY3Q+CiAgICAgICAgICAgIDxyZGY6QmFnLz4KICAgICAgICAgPC9kYzpzdWJqZWN0PgogICAgICAgICA8eG1wOk1vZGlmeURhdGU+MjAxOC0xMC0xN1QyMToxMDoxMDwveG1wOk1vZGlmeURhdGU+CiAgICAgICAgIDx4bXA6Q3JlYXRvclRvb2w+UGl4ZWxtYXRvciAzLjY8L3htcDpDcmVhdG9yVG9vbD4KICAgICAgPC9yZGY6RGVzY3JpcHRpb24+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+Crf0znkAAAHTSURBVFgJ7Za5SgNRFIbjgrjhK8TCwhfQXrETxa0RwYCVhYWgoIWgloLvoKRUsLFVX8NCRLRSK3EX1++HXDhzmYkzyZCgzIGfs9yz3ZOTm+RyGWUTqO8Emig/Ud8WgtW3UD/BetBcH22Dst8GK2m00VxhklXi7FQe0c9KufLw1pJcjmmy50CXqooWibaTUTOjpYx98DvwGgPP+KyBWNSF1wFQcUvzKLqZa+gJedw4TJoz51OOX5vYSLGbkxOgRB9ATYgK4B24ArqhGrA0gOLO4/AdGyy5wTegD4FD4PZATRXBNGgDIn0kBbAnxVAL8iDoNLYoUXl18fsoB2sfRnkAYbdUM2qu5qRF1cLapt7QZ2reiSk4hqzFVVNqZhbUnabo4AbM1aKTsKUOq5vHeBl2gK0fjIDGiPMos5Z6F1xEOVRq3yfQ7lkS+cgvmvRWfrx09zyEnf1m6/EdKv0ts3mOUXpB3I/fxerF33SK40mTuDifuwfTt0fpqvsF9KZl9LcnkNYOdTCGpLm0Q/rHEKCkSQLBJWUJvgCS5nLfsmJY0mps+quS5DG0vld+4TQexhc/aQL91PdN42HcJmk70B7p9nFIg7gFy3GcM59sAtkE/vUEfgCMe415lLSwJQAAAABJRU5ErkJggg==")

(def emacs-path "/usr/local/bin/emacs")

(def notes-path "~/Dropbox/org/notes.org")

(def args
  (->> ['(require (quote org))
        '(package-initialize)
        '(require (quote org-agenda))
        `(~'org-batch-agenda-csv "a"
          ~'org-agenda-span ~'(quote day)
          ~'org-agenda-files (~'list ~notes-path)
          ~'org-agenda-skip-deadline-if-done ~'t
          ~'org-agenda-skip-deadline-prewarning-if-scheduled ~'(quote pre-scheduled)
          ~'org-agenda-skip-scheduled-if-deadline-is-shown ~'t)]
       (map str)
       (interleave (repeat "--eval"))))


(def org-csv-keys
  [:category :head :type :todo :tags :date :time :extra :priority-1 :priority-n])

(defn deadline?
  [{:keys [type]}]
  (contains? #{"deadline" "upcoming-deadline"} type))

(defn todo-color
  [todo]
  (case todo
    "TODO" ansi/bold-red-font
    "DONE" ansi/bold-green-font))

(defn head-color
  [{:keys [type todo] :as task}]
  (if (= "DONE" todo)
    ansi/green-font
    (case type
      "scheduled" ansi/reset-font
      "past-scheduled" ansi/bold-red-font
      "upcoming-deadline" ansi/reset-font
      "deadline" ansi/red-font)))

(defn color-str
  [s color]
  (str (or color ansi/reset-font) s ansi/reset-font))

(defn bitbar-line
  [s params]
  (str s (when (seq params)
           (reduce-kv (fn [param-str param param-value]
                        (str param-str " " param "=" param-value))
                      "|"
                      params))))

(defn parse-org-csv
  [csv]
  (->> (csv/parse csv)
       (map #(map vector org-csv-keys %))
       (map #(into {} %))))

(defn emacs-batch
  []
  (let [{:keys [out exit]} (apply shell/sh emacs-path "-batch" args)]
    (if-not (zero? exit)
      (println "Bad exit code" exit))
    (let [tasks (parse-org-csv out)
          {todo "TODO", done "DONE"} (group-by :todo tasks)
          {scheduled false, deadline true} (group-by deadline? todo)]
      (println
       (bitbar-line
        (str (count todo)
             (let [deadline-count (count deadline)]
               (when-not (zero? deadline-count)
                 (str "(" deadline-count ")"))))
        {"image" icon}))
      (println "---")
      (println
       (bitbar-line
        (str (count todo)
             (let [deadline-count (count deadline)]
               (when-not (zero? deadline-count)
                 (str "(" deadline-count ")"))))
        {"image" icon}))
      (doseq [{:keys [type todo head extra] :as task} tasks]
        (println
         (bitbar-line (string/join " " [(.padStart extra 12 "_")
                                        (color-str todo (todo-color todo))
                                        (color-str head (head-color task))])
                      {"font" "Menlo"
                       "size" 12
                       "length" 100}))))))

(emacs-batch)
