;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.images
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.services.images :as si]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(def validate-form! (partial us/validate! :form/validation))
(def validate-query! (partial us/validate! :query/validation))

;; --- Create Collection

(def create-collection-scheme si/create-collection-scheme)

(defn create-collection
  [{user :identity data :data}]
  (let [data (validate-form! data create-collection-scheme)
        message (assoc data
                       :type :create/image-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/library/images/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Collection

(def update-collection-scheme si/update-collection-scheme)

(defn update-collection
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-collection-scheme)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/image-collection
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Collection

(defn delete-collection
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/image-collection
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))

;; --- List collections

(defn list-collections
  [{user :identity}]
  (let [params {:user user :type :list/image-collections}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))


