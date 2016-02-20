(ns uxbox.migrations
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg :refer (defmigration)]
            [uxbox.persistence :as up]
            [uxbox.config :as ucfg]
            [uxbox.util.template :as tmpl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Migrations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmigration txlog-0001
  "Create a initial version of txlog table."
  :up (mg/resource "migrations/0001.txlog.create.up.sql"))

(defmigration auth-0002
  "Create initial auth related tables."
  :up (mg/resource "migrations/0002.auth.tables.up.sql"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +migrations+
  {:name :uxbox-main
   :steps [[:0001 txlog-0001]
           [:0002 auth-0002]]})

(defn- migrate
  []
  (let [options (:migrations ucfg/config {})]
    (with-open [mctx (mg/context up/datasource options)]
      (mg/migrate mctx +migrations+)
      nil)))

(defstate migrations
  :start (migrate))
