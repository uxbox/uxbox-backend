;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.config
  "A configuration management."
  (:require [mount.core :as mount :refer (defstate)]
            [environ.core :refer (env)]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(def ^:const +default-config+
  "config/default.edn")

(defn read-config
  []
  (let [defaults (edn/read-string (slurp (io/resource +default-config+)))
        local (if-let [path (:uxbox-config env)]
                (io/file path)
                (io/resource "config/local.edn"))]
    (if local
      (deep-merge defaults (edn/read-string (slurp local)))
      defaults)))

(defn read-test-config
  []
  (let [defaults (edn/read-string (slurp (io/resource +default-config+)))
        local (io/resource "config/test.edn")]
    (deep-merge defaults (edn/read-string (slurp local)))))

(defstate config
  :start (read-config))
