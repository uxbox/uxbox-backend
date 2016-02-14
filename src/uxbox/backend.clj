(ns uxbox.backend
  (:require [mount.core :as mount :refer (defstate)]
            [wydra.core :as wyd]
            [wydra.rpc :as rpc]
            [uxbox.config :as cfg]
            [uxbox.backend.migrations]
            [uxbox.backend.services :as services]))

(defstate msg-conn
  :start (wyd/connect "rabbitmq://localhost/")
  :stop (.stop msg-conn))

(defstate rpc-server
  :start (rpc/server msg-conn {:handler services/-handler
                               :queue "uxbox.rpc"})
  :stop (.stop rpc-server))
