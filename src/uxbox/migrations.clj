(ns uxbox.migrations
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg]
            [uxbox.persistence :as up]
            [uxbox.migrations.misc :as mgmisc]))

(def ^:private +migrations+
  {:name :uxbox-main
   :steps [[:0001 mgmisc/txlog-0001]]})

(defn- migrate
  []
  (with-open [mctx (mg/context up/datasource)]
    (mg/migrate mctx +migrations+)
    nil))

(defstate migrations
  :start (migrate))
