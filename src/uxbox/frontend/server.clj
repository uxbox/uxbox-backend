(ns uxbox.frontend.server
  (:require [mount.core :as mount :refer (defstate)]
            [clojure.core.async :as a]
            [catacumba.core :as ct]
            [catacumba.http :as http]
            [uxbox.frontend.rpc :as rpc]))

(defn hello-world
  [context]
  (a/go
    (let [[type response] (a/<! (rpc/ask :test/test))]
      (case type
        :rpc/response
        (http/ok (:message response)
                 {:content-type "text/html; charset=utf-8"})

        :rpc/timeout
        (http/bad-request "Timeout")))))

(def app
  (ct/routes [[:all hello-world]]))

(defstate server
  :start (ct/run-server app {:port 5050})
  :stop (.stop server))

