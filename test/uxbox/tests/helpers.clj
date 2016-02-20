(ns uxbox.tests.helpers
    (:require [clojure.test :as t]
            [mount.core :as mount]
            [suricatta.core :as sc]
            [uxbox.migrations :as umg]
            [uxbox.persistence :as up]
            [uxbox.config :as ucfg]))

(def +config+ (ucfg/read-test-config))
(def +ds+ (up/create-datasource (:database +config+)))

(defn migrate
  [& args]
  (umg/migrate {:verbose false}))

(defn database-reset
  [next]
  (with-open [conn (sc/context +ds+)]
    (sc/execute conn "drop schema if exists public cascade;")
    (sc/execute conn "create schema public;"))
  (with-redefs [uxbox.migrations/migrate migrate]
    (mount/start-with {#'uxbox.config/config +config+}))
  (next))

