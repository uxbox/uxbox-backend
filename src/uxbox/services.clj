;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services
  "Main namespace for access to all uxbox services."
  (:require [suricatta.core :as sc]
            [catacumba.serializers :as sz]
            [catacumba.impl.executor :as exec]
            [clj-uuid :as uuid]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]
            [uxbox.services.auth]
            [uxbox.services.projects]
            [uxbox.services.pages]
            [uxbox.services.library]
            [uxbox.util.transit :as t]
            [uxbox.util.blob :as blob]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Impl.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private encode (comp blob/encode t/encode))

(defn- insert-txlog
  [conn data]
  (let [sql (str "INSERT INTO txlog (payload) VALUES (?)")
        sqlv [sql (encode data)]]
    (sc/execute conn sqlv)))

(defn- handle-novelty
  [data]
  (with-open [conn (up/get-conn)]
    (sc/atomic conn
      (let [result (usc/-novelty conn data)]
        (insert-txlog conn data)
        result))))

(defn- handle-query
  [data]
  (with-open [conn (up/get-conn)]
    (sc/atomic conn
      (usc/-query conn data))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn novelty
  [data]
  (exec/submit (partial handle-novelty data)))

(defn query
  [data]
  (exec/submit (partial handle-query data)))
