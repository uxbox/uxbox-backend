;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.colors
  (:require [suricatta.core :as sc]
            [buddy.core.codecs :as codecs]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.db :as db]
            [uxbox.services.core :as usc]
            [uxbox.util.transit :as t]
            [uxbox.util.blob :as blob]
            [uxbox.util.data :as data]
            [uxbox.util.uuid :as uuid]))

(def validate! (partial us/validate! :service/wrong-arguments))

(declare decode-data)

;; --- Create Collection

(defn create-collection
  [conn {:keys [id user name data]}]
  (let [sqlv (sql/create-color-collection
              {:id (or id (uuid/random))
               :name name
               :user user
               :data (blob/encode data)})]
    (-> (sc/fetch-one conn sqlv)
        (data/normalize-attrs)
        (decode-data))))

(def create-collection-schema
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]
   :data [us/required us/string]})

(defmethod usc/-novelty :create/color-collection
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params create-collection-schema)
         (create-collection conn))))

;; --- Update Collection

(defn update-collection
  [conn {:keys [id user name data version]}]
  (let [sqlv (sql/update-color-collection
              {:id id
               :user user
               :name name
               :data (blob/encode data)
               :version version})]
    (some-> (sc/fetch-one conn sqlv)
            (data/normalize-attrs)
            (decode-data))))

(def update-collection-schema
  (assoc create-collection-schema
         :version [us/required us/integer]))

(defmethod usc/-novelty :update/color-collection
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-collection-schema)
         (update-collection conn))))

;; --- Delete Collection

(defn delete-collection
  [conn {:keys [id user]}]
  (let [sqlv (sql/delete-color-collection
              {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(def delete-collection-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defmethod usc/-novelty :delete/color-collection
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params delete-collection-schema)
         (delete-collection conn))))

;; --- List Collections

(defn get-collections-for-user
  [conn user]
  (let [sqlv (sql/get-color-collections
              {:user user})]
    (->> (sc/fetch conn sqlv)
         (map data/normalize-attrs)
         (map decode-data))))

(defmethod usc/-query :list/color-collections
  [{:keys [user] :as params}]
  (with-open [conn (db/connection)]
    (get-collections-for-user conn user)))

;; --- Helpers

(defn- decode-data
  [{:keys [data] :as result}]
  (merge result (when data
                  {:data (blob/decode->str data)})))
