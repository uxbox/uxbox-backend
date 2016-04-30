;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.tasks
  "UXBOX asynchronous/scheduled tasks."
  (:require [mount.core :as mount :refer (defstate)]
            [uxbox.config :as cfg]
            [uxbox.persistence]
            [uxbox.util.quartz :as qtz]))

(def ^:private tasks
  ['uxbox.tasks.garbage/task-clean-deleted-projects])

(defn- initialize
  []
  (let [schd (qtz/scheduler)]
    (qtz/start! schd)
    (run! #(qtz/schedule! schd %) tasks)
    schd))

(defstate scheduler
  :start (initialize)
  :stop (qtz/stop! scheduler))
