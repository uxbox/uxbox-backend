(ns uxbox.main
  (:require [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry point (only for uberjar)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main
  [& args]
  (mount/start))


