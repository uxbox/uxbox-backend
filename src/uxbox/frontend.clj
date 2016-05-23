;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend
  (:require [mount.core :as mount :refer (defstate)]
            [catacumba.core :as ct]
            [uxbox.config :as cfg]
            [uxbox.frontend.routes :as urt]))

(defn- start-server
  []
  (let [config (:server cfg/config)]
    (ct/run-server (urt/app) config)))

(defstate server
  :start (start-server)
  :stop (.stop server))
