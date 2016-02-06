(ns uxbox.frontend.server
  (:require [mount.core :as mount :refer (defstate)]
            [catacumba.core :as ct]
            [catacumba.http :as http]))

(defn hello-world
  [context]
  (http/ok "<strong>Hello World</strong>"
           {:content-type "text/html; charset=utf-8"}))

(def app
  (ct/routes [[:all hello-world]]))

(defstate server
  :start (ct/run-server app {:port 5050})
  :stop (.stop server))

