(ns uxbox.frontend.routes
  (:require [catacumba.core :as ct]
            [catacumba.http :as http]
            [catacumba.serializers :as sz]
            [catacumba.handlers.auth :as cauth]
            [catacumba.handlers.postal :as pc]
            [uxbox.services.auth :as auth]
            [uxbox.frontend.core :refer (-handler)]
            [uxbox.frontend.auth]))

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

(defn app
  []
  (let [props {:secret auth/secret
               :options auth/+auth-opts+}
        backend (cauth/jwe-backend props)]
    (ct/routes [[:any (cauth/auth backend)]
                [:get "api" welcome-api]
                [:put "api" (pc/router -handler)]
                [:get "" redirect-to-api]])))

