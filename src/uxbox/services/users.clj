;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.users
  (:require [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [buddy.hashers :as hashers]
            [buddy.sign.jwe :as jwe]
            [buddy.core.nonce :as nonce]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.services.core :as usc]
            [uxbox.util.transit :as t]
            [uxbox.util.exceptions :as ex]
            [uxbox.util.blob :as blob]))

(def validate! (partial us/validate! :service/wrong-arguments))

(declare decode-user-data)
(declare find-user-by-id)
(declare find-full-user-by-id)
(declare find-user-by-username-or-email)

;; --- Retrieve User Profile (own)

(def ^:private retrieve-profile-schema
  {:user [us/required us/uuid]})

(defmethod usc/-query :retrieve/profile
  [conn params]
  (let [params (validate! params retrieve-profile-schema)]
    (some-> (find-user-by-id conn (:user params))
            (decode-user-data))))

;; --- Update User Profile (own)

(def ^:private update-profile-schema
  {:id [us/required us/uuid]
   :username [us/required us/string]
   :email [us/required us/email]
   :fullname [us/required us/string]
   :metadata [us/required us/string]})

(defn update-profile
  [conn {:keys [id username email fullname metadata]}]
  (let [metadata (blob/encode metadata)
        sqlv (sql/update-profile {:username username
                                  :fullname fullname
                                  :metadata metadata
                                  :email email
                                  :id id})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (decode-user-data)
            (dissoc :password))))

(defmethod usc/-novelty :update/profile
  [conn params]
  (some->> (validate! params update-profile-schema)
           (update-profile conn)))

;; --- Update Password

(def ^:private update-password-schema
  {:user [us/required us/uuid]
   :password [us/required us/string]
   :old-password [us/required us/string]})

(defn update-password
  [conn {:keys [user password]}]
  (let [password (hashers/encrypt password)
        sqlv (sql/update-profile-password {:id user :password password})]
    (pos? (sc/execute conn sqlv))))

(defn- validate-old-password
  [conn {:keys [user old-password] :as params}]
  (let [user (find-full-user-by-id conn user)]
    (when-not (hashers/check old-password (:password user))
      (let [params {:old-password '("errors.api.form.old-password-not-match")}]
        (throw (ex/ex-info :form/validation params))))
    params))

(defmethod usc/-novelty :update/password
  [conn params]
  (->> (validate! params update-password-schema)
       (validate-old-password conn)
       (update-password conn)))

;; --- Create User

(def ^:private create-user-schema
  {:id [us/uuid]
   :metadata [us/required us/string]
   :username [us/required us/string]
   :fullname [us/required us/string]
   :email [us/required us/email]
   :password [us/required us/string]})

(defn create-user
  [conn {:keys [id username password email fullname metadata] :as data}]
  (validate! data create-user-schema)
  (let [metadata (blob/encode metadata)
        id (or id (uuid/v4))
        sqlv (sql/create-profile {:id id
                                  :fullname fullname
                                  :username username
                                  :email email
                                  :password password
                                  :metadata metadata})]
    (->> (sc/fetch-one conn sqlv)
         (usc/normalize-attrs)
         (decode-user-data))))

;; --- Helpers

(defn find-full-user-by-id
  [conn id]
  (let [sqlv (sql/get-profile {:id id})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn find-user-by-id
  [conn id]
  (let [sqlv (sql/get-profile {:id id})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (dissoc :password))))

(defn find-user-by-username-or-email
  [conn username]
  (let [sqlv (sql/get-profile-by-username {:username username})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn- decode-user-data
  [{:keys [metadata] :as result}]
  (merge result (when metadata
                  {:metadata (blob/decode->str metadata)})))
