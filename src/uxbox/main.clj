;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.main
  (:require [clojure.tools.namespace.repl :as repl]
            [clojure.walk :refer [macroexpand-all]]
            [clojure.pprint :refer [pprint]]
            [clojure.test :as test]
            [mount.core :as mount]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [uxbox.config :as cfg]
            [uxbox.migrations]
            [uxbox.persistence]
            [uxbox.frontend])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Development Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- start
  []
  (mount/start))

(defn- stop
  []
  (mount/stop))

(defn- refresh
  []
  (stop)
  (repl/refresh))

(defn- refresh-all
  []
  (stop)
  (repl/refresh-all))

(defn- go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn- reset
  []
  (stop)
  (repl/refresh :after 'uxbox.main/start))

(defn make-secret
  []
  (let [rdata (nonce/random-bytes 64)]
    (codecs/bytes->safebase64 rdata)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point (only for uberjar)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce +vars+ nil)

(defn run-all-tests
  ([] (test/run-all-tests #"^uxbox.tests.*"))
  ([re] (test/run-all-tests re)))

(defn test-vars
  []
  (test/test-vars +vars+))

(defn run-tests
  [& vars]
  (cond
    (pos? (count vars))
    (do
      (alter-var-root #'+vars+ (constantly vars))
      (repl/refresh :after 'uxbox.main/test-vars))

    :else
    (repl/refresh :after 'uxbox.main/run-all-tests)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point (only for uberjar)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (mount/start))


