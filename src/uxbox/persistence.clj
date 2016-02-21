(ns uxbox.persistence
  (:require [mount.core :as mount :refer (defstate)]
            [hikari-cp.core :as hikari]
            [suricatta.core :as sc]
            [uxbox.config :as cfg]))

(def ^:const +defaults+
  {:connection-timeout 30000
   :idle-timeout 600000
   :max-lifetime 1800000
   :minimum-idle 10
   :maximum-pool-size 10
   :adapter "postgresql"
   :username ""
   :password ""
   :database-name ""
   :server-name "localhost"
   :port-number 5432})

(defn create-datasource
  [config]
  (let [dbconf (merge +defaults+ config)]
    (hikari/make-datasource dbconf)))

(defstate datasource
  :start (create-datasource (:database cfg/config))
  :stop (hikari/close-datasource datasource))

(defn get-conn
  []
  (sc/context datasource))
