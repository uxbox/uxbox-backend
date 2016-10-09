;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.colors
  (:require [suricatta.core :as sc]
            [buddy.core.codecs :as codecs]
            [clojure.spec :as s]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.db :as db]
            [uxbox.services.core :as core]
            [uxbox.util.transit :as t]
            [uxbox.util.blob :as blob]
            [uxbox.util.data :as data]
            [uxbox.util.uuid :as uuid]))

(declare decode-data)

(s/def ::user uuid?)
(s/def ::data string?)
(s/def ::collection uuid?)

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

(s/def ::create-color-collection
  (s/keys :req-un [::user ::us/name ::data]
          :opt-un [::us/id]))

(defmethod core/novelty :create-color-collection
  [params]
  (s/assert ::create-color-collection params)
  (with-open [conn (db/connection)]
    (create-collection conn params)))

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

(s/def ::update-color-collection
  (s/keys :req-un [::user ::us/name ::data ::us/version]
          :opt-un [::us/id]))

(defmethod core/novelty :update-color-collection
  [params]
  (s/assert ::update-color-collection params)
  (with-open [conn (db/connection)]
    (update-collection conn params)))

;; --- Delete Collection

(defn delete-collection
  [conn {:keys [id user]}]
  (let [sqlv (sql/delete-color-collection
              {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(s/def ::delete-collection-schema
  (s/keys :req-un [::us/id ::user]))

(defmethod core/novelty :delete-color-collection
  [params]
  (s/assert ::delete-collection-schema params)
  (with-open [conn (db/connection)]
    (delete-collection conn params)))

;; --- List Collections

(defn get-collections-for-user
  [conn user]
  (let [sqlv (sql/get-color-collections {:user user})]
    (->> (sc/fetch conn sqlv)
         (map data/normalize-attrs)
         (map decode-data))))

(defmethod core/query :list-color-collections
  [{:keys [user] :as params}]
  (s/assert ::user user)
  (with-open [conn (db/connection)]
    (get-collections-for-user conn user)))

;; --- Helpers

(defn- decode-data
  [{:keys [data] :as result}]
  (s/assert ::us/bytes data)
  (merge result (when data
                  {:data (blob/decode->str data)})))
