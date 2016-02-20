(ns uxbox.services.auth
  (:require [suricatta.core :as sc]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]))

(defmethod usc/-novelty :auth/login
  [{:keys [username password scope]}]
  (println "hello world"))
