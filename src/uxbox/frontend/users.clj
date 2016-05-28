;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.users
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [storages.core :as st]
            [uxbox.media :as media]
            [uxbox.images :as images]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.services.users :as svu]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]
            [uxbox.util.paths :as paths]))

;; --- Helpers

(def validate-form! (partial us/validate! :form/validation))

(defn- resolve-thumbnail
  [user]
  (let [opts {:src :photo
              :dst :photo
              :size [100 100]
              :quality 90
              :format "jpg"}]
    (images/populate-thumbnails user opts)))

;; --- Retrieve Profile

(defn retrieve-profile
  [{user :identity}]
  (let [message {:user user
                 :type :retrieve/profile}]
    (->> (sv/query message)
         (p/map resolve-thumbnail)
         (p/map #(http/ok (rsp %))))))

;; --- Update Profile

(def ^:private update-profile-schema
  {:id [us/required us/uuid]
   :username [us/required us/string]
   :email [us/required us/email]
   :fullname [us/required us/string]
   :metadata [us/required us/string]})

(defn update-profile
  [{user :identity data :data}]
  (let [data (validate-form! data update-profile-schema)
        message (assoc data
                       :type :update/profile
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Update Password

(def ^:private update-password-schema
  {:password [us/required us/string [us/min-len 6]]
   :old-password [us/required us/string]})

(defn update-password
  [{user :identity data :data}]
  (let [data (validate-form! data update-password-schema)
        message (assoc data
                      :type :update/password
                      :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Update Profile Photo

(defn update-photo
  [{user :identity data :data}]
  (letfn [(store-photo [file]
            (let [filename (paths/base-name file)
                  storage media/images-storage]
              (st/save storage filename file)))
          (assign-photo [path]
            (sv/novelty {:user user
                         :path (str path)
                         :type :update/profile-photo}))
          (create-response [_]
            (http/no-content))]
    (->> (store-photo (:file data))
         (p/mapcat assign-photo)
         (p/map create-response))))

;; --- Register User

(def ^:private register-user-schema
  svu/register-user-schema)

(defn register-user
  [{data :data}]
  (let [data (validate-form! data register-user-schema)
        message (assoc data :type :register)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))


;; --- Request Password Recovery

(def ^:private request-password-recovery-schema
  {:username [us/required us/string]})

(defn request-password-recovery
  [{data :data}]
  (let [data (validate-form! data request-password-recovery-schema)
        message (assoc data :type :request/password-recovery)]
    (->> (sv/novelty message)
         (p/map (fn [_] (http/no-content))))))

;; --- Password Recovery

(def ^:private password-recovery-schema
  {:token [us/required us/string]
   :password [us/required us/string]})

(defn recover-password
  [{data :data}]
  (let [data (validate-form! data password-recovery-schema)
        message (assoc data :type :recover/password)]
    (->> (sv/novelty message)
         (p/map (fn [_] (http/no-content))))))

;; --- Valiadate Recovery Token

(defn validate-recovery-token
  [{params :route-params}]
  (let [message {:type :validate/password-recovery-token
                 :token (:token params)}]
    (->> (sv/query message)
         (p/map (fn [v]
                  (if v
                    (http/no-content)
                    (http/not-found "")))))))
