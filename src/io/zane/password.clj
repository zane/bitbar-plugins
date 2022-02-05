(ns io.zane.password
  (:refer-clojure :exclude [get])
  (:require [clojure.core.match :as match]
            [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defn add-generic
  [service account password]
  (shell/sh "security" "add-generic-password" "-s" service "-a" account "-w" password))

(defn find-generic
  [service account]
  (match/match (shell/sh "security" "find-generic-password" "-w" "-s" service "-a" account)
    {:exit 0 :out out}
    (string/trim out)))
