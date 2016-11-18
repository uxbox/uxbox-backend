;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.config
  "A configuration management."
  (:require [mount.core :refer [defstate]]
            [environ.core :refer (env)]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [uxbox.util.data :refer [deep-merge]]))


(def ^:dynamic *default-config-path* "config/default.edn")
(def ^:dynamic *local-config-path* "config/local.edn")

(defn read-config
  []
  (let [builtin (io/resource *default-config-path*)
        local (io/resource *local-config-path*)
        external (io/file (:uxbox-config env))]
    (deep-merge (edn/read-string (slurp builtin))
                (when local (edn/read-string (slurp local)))
                (when (and external (.exists external))
                  (edn/read-string (slurp external))))))

(defn read-test-config
  []
  (binding [*local-config-path* "config/test.edn"]
    (read-config)))

(defstate config
  :start (read-config))
