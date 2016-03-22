;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.main
  (:require [clojure.tools.namespace.repl :as repl]
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

(defn -main
  [& args]
  (mount/start))


