;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.users
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(def validate-form! (partial us/validate! :form/validation))

;; --- Retrieve Profile

(defn retrieve-profile
  [{user :identity}]
  (let [params {:user user
                :type :retrieve/profile}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

;; --- Update Profile

(def ^:private update-profile-schema
  {:id [us/required us/uuid]
   :username [us/required us/string]
   :email [us/required us/email]
   :fullname [us/required us/string]
   :metadata [us/coll]})

(defn update-profile
  [{user :identity data :data}]
  (let [params (validate-form! data update-profile-schema)
        message (assoc params
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
  (let [params (validate-form! data update-password-schema)
        message (assoc params
                      :type :update/password
                      :user user)]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

