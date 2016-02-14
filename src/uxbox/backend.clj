(ns uxbox.backend
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg]
            [uxbox.config :as cfg]
            [uxbox.backend.migrations :as umg]))

(def ^:private +backend-migrations+
  {:name :uxbox-backend
   :steps [[:0001 umg/migration-0001-auth-table]]})

(defn- migrate
  []
  (let [dbspec (:database cfg/config)]
    (with-open [mctx (mg/context dbspec)]
      (mg/migrate mctx +backend-migrations+)
      nil)))

(defstate migrations
  :start (migrate))
