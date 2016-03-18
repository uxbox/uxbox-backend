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
            [uxbox.frontend.errors :as ufe]
            [uxbox.frontend.projects :as ufp])
  (:import java.util.UUID))

(defn- welcome-api
  "A GET entry point for the api that shows
  a welcome message."
  [context]
  (let [body {:message "Welcome to UXBox api."}]
    (-> (sz/encode body :json)
        (http/ok {:content-type "application/json"}))))

(defn- authorization
  [{:keys [identity] :as context}]
  (if identity
    (ct/delegate {:identity (UUID/fromString (:id identity))})
    (http/forbidden (ufc/rsp {:message "Forbidden"}))))

(def cors-conf
  {:origin "*"
   :max-age 3600
   :allow-methods #{:post :put :get :delete :trace}
   :allow-headers #{:x-requested-with :content-type :authorization}})

(defn app
  []
  (let [props {:secret auth/secret
               :options auth/+auth-opts+}
        backend (cauth/jwe-backend props)]
    (ct/routes
     [[:any (cauth/auth backend)]
      [:any (cmisc/autoreloader)]
      [:get "api" #'welcome-api]
      [:prefix "api"
       [:any (cmisc/cors cors-conf)]
       [:any (cparse/body-params)]
       [:error #'ufe/handler]

       [:post "auth/token" #'ufa/login]
       [:any #'authorization]

       ;; Projects
       [:get "projects/:id/pages" #'ufp/list-pages-by-project]
       [:put "projects/:id" #'ufp/update-project]
       [:delete "projects/:id" #'ufp/delete-project]
       [:post "projects" #'ufp/create-project]
       [:get "projects" #'ufp/list-projects]

       ;; Pages
       [:put "pages/:id/metadata" #'ufp/update-page-metadata]
       [:put "pages/:id" #'ufp/update-page]
       [:delete "pages/:id" #'ufp/delete-page]
       [:post "pages" #'ufp/create-page]
       [:get "pages" #'ufp/list-pages]]])))
