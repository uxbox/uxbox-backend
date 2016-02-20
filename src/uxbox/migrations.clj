(ns uxbox.migrations
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg]
            [uxbox.persistence :as up]
            [uxbox.config :as ucfg]
            [uxbox.migrations.misc :as mgmisc]))

(def ^:private +migrations+
  {:name :uxbox-main
   :steps [[:0001 mgmisc/txlog-0001]]})

(defn migrate
  []
  (let [options (:migrations ucfg/config {})]
    (with-open [mctx (mg/context up/datasource options)]
      (mg/migrate mctx +migrations+)
      nil)))

(defstate migrations
  :start (migrate))
