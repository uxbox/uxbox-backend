;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.tasks.garbage
  "Garbage Collector related tasks."
  (:require [suricatta.core :as sc]
            [uxbox.persistence :as ps]))

;; --- Delete projects

(defn- clean-deleted-projects
  [conn]
  (let [sql (str "DELETE FROM projects "
                 " WHERE deleted=true AND "
                 "       (now()-deleted_at)::interval > '10 day'::interval;")]
    (sc/execute conn sql)))

(defn task-clean-deleted-projects
  "Task that cleans the deleted projects."
  {:repeat? true
   :interval 3600}
  []
  (with-open [conn (ps/get-conn)]
    (sc/atomic conn
      (clean-deleted-projects conn))))
