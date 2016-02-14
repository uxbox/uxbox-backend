(ns uxbox.frontend.rpc
  (:require [mount.core :as mount :refer (defstate)]
            [wydra.core :as wyd]
            [wydra.rpc :as rpc]
            [uxbox.config :as cfg])
  (:gen-class))

(defstate msg-conn
  :start (wyd/connect "rabbitmq://localhost/")
  :stop (.stop msg-conn))

(defstate rpc-client
  :start (rpc/client msg-conn {:queue "uxbox.rpc"})
  :stop (.stop rpc-client))

(defn ask
  ([cmd]
   (ask cmd nil))
  ([cmd data]
   (let [msg (merge {:cmd cmd} data)]
     (rpc/ask rpc-client msg))))
