(ns io.zane.applescript
  (:require [clojure.java.shell :as shell]
            #?@(:bb [] :clj [[cljs.closure :as cljs]])))

(defn run
  [path & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" path rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))

(defn run-js
  [path & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" "-l" "JavaScript" path rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))

(defn run-js-str
  [s & rest]
  (let [{:keys [exit] :as result} (apply shell/sh "osascript" "-l" "JavaScript" :in s rest)]
    (if (zero? exit)
      result
      (throw (ex-info "AppleScript returned a non-zero error code." result)))))

#?(:bb :ignore
   :clj
   (defn run-cljs
     [form & rest]
     (let [form (list 'pr (list 'js->clj form))
           js (cljs/build form {:optimizations :simple})
           {:keys [exit] :as result} (apply shell/sh "osascript" "-l" "JavaScript" :in js rest)]
       (if (zero? exit)
         result
         (throw (ex-info "AppleScript returned a non-zero error code." result))))))

(comment

  (set! *print-length* 10)

  (run-cljs '(for [todo (-> (js/Application "Things")
                            (.-lists)
                            (.byName "Today")
                            (.toDos))]
               (.makeJSONFrom
                (js/Application "JSON Helper")
                todo)
               #_
               {:id (.id todo)
                :name (.name todo)
                :due-date (.dueDate todo)
                :status (.status todo)}))

  (type '(-> (js/Application "Location Helper")
             (.getLocationCoordinates)))

  (set! *print-length* 10)
  (cljs/build '(println "Hello") {:optimizations :simple})

  ,)
