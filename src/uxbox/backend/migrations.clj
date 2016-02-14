(ns uxbox.backend.migrations
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg]
            [uxbox.config :as cfg]))

(defn- auth-table-up
  [ctx]
  )

(defn- auth-table-down
  [ctx]
  )

(def migration-0001-auth-table
  {:up auth-table-up
   :down auth-table-down})

(def ^:private +backend-migrations+
  {:name :uxbox-backend
   :steps [[:0001 migration-0001-auth-table]]})

(defn- migrate
  []
  (let [dbspec (:database cfg/config)]
    (with-open [mctx (mg/context dbspec)]
      (mg/migrate mctx +backend-migrations+)
      nil)))

(defstate migrations
  :start (migrate))

