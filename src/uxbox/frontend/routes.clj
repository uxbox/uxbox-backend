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
            [catacumba.handlers.parse :as cparse]
            [catacumba.handlers.misc :as cmisc]
            [uxbox.services.auth :as auth]
            [uxbox.frontend.core :as ufc]
            [uxbox.frontend.auth :as ufa]
            [uxbox.frontend.projects :as ufp])
  (:import java.util.UUID))

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

(defn- authorization
  [{:keys [identity] :as context}]
  (if identity
    (ct/delegate {:identity (UUID/fromString (:id identity))})
    (http/forbidden (ufc/rsp {:message "Forbidden"}))))

(defn- error-handler
  [context err]
  (if (instance? clojure.lang.ExceptionInfo err)
    (let [message (.getMessage err)
          data (ex-data err)]
      (-> (ufc/rsp {:message message
                    :payload data})
          (http/bad-request)))
    (let [message (.getMessage err)]
      (-> (ufc/rsp {:message message})
          (http/internal-server-error)))))

(defn app
  []
  (let [props {:secret auth/secret
               :options auth/+auth-opts+}
        backend (cauth/jwe-backend props)]
    (ct/routes [[:any (cauth/auth backend)]
                [:any (cmisc/autoreloader)]
                [:prefix "api"
                 [:any (cparse/body-params)]
                 [:error #'error-handler]
                 [:post "auth/token" #'ufa/login]
                 [:get "" #'welcome-api]]
                [:get "" #'redirect-to-api]])))
