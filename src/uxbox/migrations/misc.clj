(ns uxbox.migrations.misc
  "A namespace for define the core uxbox migrations
  procedures like a transaction log."
  (:require [migrante.core :as mg :refer (defmigration)]
            [uxbox.util.template :as tmpl]))

(defmigration txlog-0001
  "A initial migration for the transaction log used mainly
  by the persistence module."
  :up (let [tmpl (tmpl/render "migrations/txlog-0001-up.sql")]
        (mg/execute tmpl)))
