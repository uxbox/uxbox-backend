(ns uxbox.backend.migrations
  (:require [migrante.core :as mg]))

(defn- auth-table-up
  [ctx]
  )

(defn- auth-table-down
  [ctx]
  )

(def migration-0001-auth-table
  {:up auth-table-up
   :down auth-table-down})
