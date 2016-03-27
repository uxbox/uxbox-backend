;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.library
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(defn list-color-collections
  [{user :identity}]
  (let [params {:user user
                :type :list/color-collections}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

(defn create-color-collection
  [{user :identity params :data}]
  (p/alet [params (assoc params
                         :type :create/color-collection
                         :user user)
           result (p/await (sv/novelty params))
           loc (str "/api/library/colors/" (:id result))]
    (http/created loc (rsp result))))

(defn update-color-collection
  [{user :identity params :route-params data :data}]
  (let [params (assoc data
                      :id (uuid/from-string (:id params))
                      :type :update/color-collection
                      :user user)]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

(defn delete-color-collection
  [{user :identity params :route-params}]
  (let [params {:id (uuid/from-string (:id params))
                :type :delete/color-collection
                :user user}]
    (-> (sv/novelty params)
        (p/then (fn [v] (http/no-content))))))
