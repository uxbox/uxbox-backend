;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.auth
  (:require [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [buddy.hashers :as hashers]
            [buddy.sign.jwe :as jwe]
            [buddy.core.nonce :as nonce]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]))

(def ^:const +auth-opts+
  {:alg :a256kw :enc :a256cbc-hs512})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- initialize-auth-secret
  []
  (let [main-secret (:secret ucfg/config)]
    (when-not main-secret
      (throw (ex-info "Missing `:secret` key in config." {})))
    (hash/blake2b-256 main-secret)))

(defstate secret
  :start (initialize-auth-secret))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +user-schema+
  {:id [us/uuid]
   :username [us/required us/string]
   :email [us/required us/email]
   :password [us/required us/string]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repository
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-user
  [conn {:keys [id username password email] :as data}]
  {:pre [(us/validate! data +user-schema+)]}
  (let [sql (str "INSERT INTO users (id, username, email, password)"
                 " VALUES (?, ?, ?, ?) RETURNING *;")
        id (or id (uuid/v4))
        sqlv [sql id username email password]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn find-user-by-id
  [conn id]
  (let [sql (str "SELECT * FROM users WHERE id=?")]
    (some-> (sc/fetch-one conn [sql id])
            (usc/normalize-attrs))))

(defn find-user-by-username-or-email
  [conn username]
  (let [sql (str "SELECT * FROM users WHERE username=? OR email=?")
        sqlv [sql username username]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- check-user-password
  [user password]
  (hashers/check password (:password user)))

(defn generate-token
  [user]
  (let [data {:id (:id user)}]
    (jwe/encrypt data secret +auth-opts+)))

(defmethod usc/-novelty :auth/login
  [conn {:keys [username password scope]}]
  (let [user (find-user-by-username-or-email conn username)]
    (when-not user (throw (ex-info "errors.api.auth.invalid-credentials" {})))
    (if (check-user-password user password)
      {:token (generate-token user)}
      (throw (ex-info "errors.api.auth.invalid-credentials" {})))))
