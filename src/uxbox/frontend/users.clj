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

(defn retrieve-profile
  [{user :identity}]
  (let [params {:user user
                :type :retrieve/profile}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

(defn update-profile
  [{user :identity data :data}]
  (let [params (assoc data
                      :type :update/profile
                      :user user)]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

(defn update-password
  [{user :identity data :data}]
  (let [params (assoc data
                      :type :update/password
                      :id user)]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

