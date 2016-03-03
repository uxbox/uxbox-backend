;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

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

