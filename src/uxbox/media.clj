;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media
  "A media storage impl for uxbox."
  (:require [mount.core :as mount :refer (defstate)]
            [clojure.java.io :as io]
            [cuerdas.core :as str]
            [storages.core :as st]
            [storages.fs :refer (filesystem)]
            [storages.misc :refer (hashed prefixed)]
            [uxbox.config :refer (config)]))

(defn- resolve-basedir
  [^String basedir]
  (or (io/resource basedir)
      basedir))

(defn- initialize-storage
  [{:keys [basedir baseuri] :as config}]
  (let [basedir (resolve-basedir basedir)]
    (-> (filesystem {:basedir basedir
                     :baseuri baseuri})
        (prefixed "images")
        (hashed))))

(defstate storage
  :start (initialize-storage (:storage config)))
