;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.auth
  (:require [clojure.spec :as s]
            [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [uxbox.config :as cfg]
            [uxbox.schema :as us]
            [uxbox.db :as db]
            [uxbox.services.core :as core]
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

(s/def ::scope string?)
(s/def ::login
  (s/keys :req-un [::us/username ::us/password ::scope]))

(defmethod core/novelty :login
  [{:keys [username password scope] :as params}]
  (s/assert ::login params)
  (with-open [conn (db/connection)]
    (let [user (users/find-user-by-username-or-email conn username)]
      (when-not user
        (throw (ex/ex-info :auth/wrong-credentials {})))
      (if (check-user-password user password)
        {:token (generate-token user)}
        (throw (ex/ex-info :auth/wrong-credentials {}))))))
