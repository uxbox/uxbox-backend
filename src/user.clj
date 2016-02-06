(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [clojure.pprint :refer [pprint]]
            [mount.core :as mount]
            [uxbox.frontend.server]))

(defn start
  []
  (mount/start))
  ;; (mount/start #'app.conf/config
  ;;              #'app.db/conn
  ;;              #'app.www/nyse-app
  ;;              #'app.example/nrepl))             ;; example on how to start app with certain states

(defn stop
  []
  (mount/stop))

(defn refresh
  []
  (stop)
  (repl/refresh))

(defn refresh-all
  []
  (stop)
  (repl/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn reset
  []
  (stop)
  (repl/refresh :after 'user/start))

(mount/in-clj-mode)
