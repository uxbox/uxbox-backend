;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.auth
  (:require [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [uxbox.config :as cfg]
            [uxbox.db :as db]
            [uxbox.services.core :as usc]
            [uxbox.services.users :as users]
            [uxbox.util.exceptions :as ex]))

(def ^:const +auth-opts+
  {:alg :a256kw :enc :a256cbc-hs512})

;; --- State

(defn- initialize-auth-secret
  []
  (let [main-secret (:secret cfg/config)]
    (when-not main-secret
      (throw (ex-info "Missing `:secret` key in config." {})))
    (hash/blake2b-256 main-secret)))

(defstate secret
  :start (initialize-auth-secret))

;; --- Login

(defn- check-user-password
  [user password]
  (hashers/check password (:password user)))

(defn generate-token
  [user]
  (let [data {:id (:id user)}]
    (jwt/encrypt data secret +auth-opts+)))

(defmethod usc/-novelty :auth/login
  [{:keys [username password scope]}]
  (with-open [conn (db/connection)]
    (let [user (users/find-user-by-username-or-email conn username)]
      (when-not user
        (throw (ex/ex-info :auth/wrong-credentials {})))
      (if (check-user-password user password)
        {:token (generate-token user)}
        (throw (ex/ex-info :auth/wrong-credentials {}))))))
