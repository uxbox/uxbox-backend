(ns uxbox.frontend.server
  (:require [mount.core :as mount :refer (defstate)]
            [catacumba.core :as ct]
            [catacumba.http :as http]
            [catacumba.serializers :as sz]
            [catacumba.handlers.postal :as pc]
            [uxbox.frontend.handler :refer (-handler)]
            [uxbox.frontend.handler.auth]))

;; (defn hello-world
;;   [context]
;;   (a/go
;;     (let [[type response] (a/<! (rpc/ask :test/test))]
;;       (case type
;;         :rpc/response
;;         (http/ok (:message response)
;;                  {:content-type "text/html; charset=utf-8"})

;;         :rpc/timeout
;;         (http/bad-request "Timeout")))))

(defn- welcome-api
  "A GET entry point for the api that shows
  a welcome message."
  [context]
  (let [body {:message "Welcome to UXBox api."}]
    (-> (sz/encode body :json)
        (http/ok {:content-type "application/json"}))))

(defn- redirect-to-api
  "Endpoint that just redirect to the /api endpoint."
  [context]
  (http/see-other "/api"))

(def ^:private app
  (ct/routes [[:get "api" welcome-api]
              [:post "api" (pc/router -handler)]
              [:get "" redirect-to-api]]))

(defstate server
  :start (ct/run-server app {:port 5050})
  :stop (.stop server))

