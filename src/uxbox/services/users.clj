;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.users
  (:require [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [buddy.hashers :as hashers]
            [buddy.sign.jwe :as jwe]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.db :as db]
            [uxbox.emails :as emails]
            [uxbox.services.core :as usc]
            [uxbox.util.transit :as t]
            [uxbox.util.exceptions :as ex]
            [uxbox.util.blob :as blob]
            [uxbox.util.uuid :as uuid]
            [uxbox.util.token :as token]))

(def validate! (partial us/validate! :service/wrong-arguments))

(declare decode-user-data)
(declare trim-user-attrs)
(declare find-user-by-id)
(declare find-full-user-by-id)
(declare find-user-by-username-or-email)

;; --- Retrieve User Profile (own)

(def ^:private retrieve-profile-schema
  {:user [us/required us/uuid]})

(defmethod usc/-query :retrieve/profile
  [params]
  (let [params (validate! params retrieve-profile-schema)]
    (with-open [conn (db/connection)]
      (some-> (find-user-by-id conn (:user params))
              (decode-user-data)))))

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
            (trim-user-attrs)
            (decode-user-data)
            (dissoc :password))))

(defmethod usc/-novelty :update/profile
  [params]
  (with-open [conn (db/connection)]
    (some->> (validate! params update-profile-schema)
             (update-profile conn))))

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
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-password-schema)
         (validate-old-password conn)
         (update-password conn))))

;; --- Update Photo

(defn update-photo
  [conn {:keys [user path]}]
  (let [sqlv (sql/update-profile-photo {:id user :photo path})]
    (pos? (sc/execute conn sqlv))))

(def update-photo-schema
  {:user [us/required us/uuid]
   :path [us/required us/string]})

(defmethod usc/-novelty :update/profile-photo
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-photo-schema)
         (update-photo conn))))

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
  (let [id (or id (uuid/random))
        metadata (blob/encode metadata)
        password (hashers/encrypt password)
        sqlv (sql/create-profile {:id id
                                  :fullname fullname
                                  :username username
                                  :email email
                                  :password password
                                  :metadata metadata})]
    (->> (sc/fetch-one conn sqlv)
         (usc/normalize-attrs)
         (trim-user-attrs)
         (decode-user-data))))

;; --- Register User

(defn- user-registred?
  "Check if the user identified by username or by email
  is already registred in the platform."
  [conn {:keys [username email]}]
  (or (find-user-by-username-or-email conn username)
      (find-user-by-username-or-email conn email)))

(defn- register-user
  "Create the user entry onthe database with limited input
  filling all the other fields with defaults."
  [conn {:keys [username fullname email password] :as params}]

  (when (user-registred? conn params)
    (throw (ex/ex-info :validation {})))

  (let [metadata (blob/encode (t/encode {}))
        password (hashers/encrypt password)
        sqlv (sql/create-profile {:id (uuid/random)
                                  :fullname fullname
                                  :username username
                                  :email email
                                  :password password
                                  :metadata metadata})]
    (sc/execute conn sqlv)
    (emails/send! {:email/name :register
                   :email/to (:email params)
                   :email/priority :high
                   :name (:fullname params)})
    nil))

(def register-user-schema
  {:username [us/required us/string]
   :fullname [us/required us/string]
   :email [us/required us/email]
   :password [us/required us/string]})

(defmethod usc/-novelty :register
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params register-user-schema)
         (sc/apply-atomic conn register-user-schema))))

;; --- Password Recover

(defn- recovery-token-exists?
  "Checks if the token exists in the system. Just
  return `true` or `false`."
  [conn token]
  (let [sqlv (sql/recovery-token-exists? {:token token})
        result (sc/fetch-one conn sqlv)]
    (:token_exists result)))

(defn- retrieve-user-for-recovery-token
  "Retrieve a user id (uuid) for the given token. If
  no user is found, an exception is raised."
  [conn token]
  (let [sqlv (sql/get-recovery-token {:token token})
        data (sc/fetch-one conn sqlv)]
    (if-let [user (:user data)]
      user
      (throw (ex/ex-info :service/not-found {:token token})))))

(defn- mark-token-as-used
  [conn token]
  (let [sqlv (sql/mark-recovery-token-used {:token token})]
    (pos? (sc/execute conn sqlv))))

(defn- recover-password
  "Given a token and password, resets the password
  to corresponding user or raise an exception."
  [conn {:keys [token password]}]
  (let [user (retrieve-user-for-recovery-token conn token)]
    (update-password conn {:user user :password password})
    (mark-token-as-used conn token)
    nil))

(defn- create-recovery-token
  "Creates a new recovery token for specified user and return it."
  [conn userid]
  (let [token (token/random)
        sqlv (sql/create-recovery-token {:user userid
                                         :token token})]
    (sc/execute conn sqlv)
    token))

(defn- request-password-recovery
  "Creates a new recovery password token and sends it via email
  to the correspondig to the given username or email address."
  [conn username]
  (let [user (find-user-by-username-or-email conn username)
        _    (when-not user
               (ex/ex-info :service/not-found {:username username}))
        token (create-recovery-token conn (:id user))]
    (emails/send! {:email/name :password-recovery
                   :email/to (:email user)
                   :name (:fullname user)
                   :token token})
    token))

(defmethod usc/-query :validate/password-recovery-token
  [{:keys [token]}]
  (with-open [conn (db/connection)]
    (recovery-token-exists? conn token)))

(defmethod usc/-novelty :request/password-recovery
  [{:keys [username]}]
  (with-open [conn (db/connection)]
    (sc/atomic conn
      (request-password-recovery conn username))))

(defmethod usc/-novelty :recover/password
  [{:keys [token password] :as params}]
  (with-open [conn (db/connection)]
    (sc/atomic conn
      (recover-password conn params))))

;; --- Query Helpers

(defn find-full-user-by-id
  "Find user by its id. This function is for internal
  use only because it returns a lot of sensitive information.
  If no user is found, `nil` is returned."
  [conn id]
  (let [sqlv (sql/get-profile {:id id})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn find-user-by-id
  "Find user by its id. If no user is found, `nil` is returned."
  [conn id]
  (let [sqlv (sql/get-profile {:id id})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (trim-user-attrs)
            (dissoc :password))))

(defn find-user-by-username-or-email
  "Finds a user in the database by username and email. If no
  user is found, `nil` is returned."
  [conn username]
  (let [sqlv (sql/get-profile-by-username {:username username})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (trim-user-attrs))))

;; --- Attrs Helpers

(defn- decode-user-data
  [{:keys [metadata] :as result}]
  (merge result (when metadata
                  {:metadata (blob/decode->str metadata)})))

(defn trim-user-attrs
  "Only selects a publicy visible user attrs."
  [user]
  (select-keys user [:id :username :fullname
                     :password :metadata :email
                     :created-at]))

