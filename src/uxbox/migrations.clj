(ns uxbox.migrations
  (:require [mount.core :as mount :refer (defstate)]
            [migrante.core :as mg]
            [uxbox.config :as cfg]
            [uxbox.migrations.misc :as mgmisc]))

(def ^:private +migrations+
  {:name :uxbox-main
   :steps [[:0001 mgmisc/txlog-0001]]})

(defn- migrate
  []
  (let [dbspec (:database cfg/config)]
    (with-open [mctx (mg/context dbspec)]
      (mg/migrate mctx +migrations+)
      nil)))

(defstate migrations
  :start (migrate))

