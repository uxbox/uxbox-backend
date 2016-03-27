;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.library
  (:require [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.util.transit :as t]
            [uxbox.services.core :as usc]))

(declare decode-data)

;; --- Create Color Collection

(def ^:private create-color-coll-schema
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]
   :data [us/required us/set]})

(defn create-color-collection
  [conn {:keys [id user name data]}]
  (let [data (codecs/bytes->str (t/encode data))
        sql (str "INSERT INTO color_collections (id, \"user\", name, data) "
                  " VALUES (?, ?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user name data]]
    (-> (sc/fetch-one conn sqlv)
        (usc/normalize-attrs)
        (decode-data))))

(defmethod usc/-novelty :create/color-collection
  [conn params]
  (->> (usc/validate! params create-color-coll-schema)
       (create-color-collection conn)))

;; --- Update Color Collection

(def ^:private update-color-coll-schema
  (assoc create-color-coll-schema
         :version [us/required us/integer]))

(defn update-color-collection
  [conn {:keys [id user name data version]}]
  (let [data (codecs/bytes->str (t/encode data))
        sql (str "UPDATE color_collections SET name=?,data=?,version=? "
                 " WHERE id=? AND \"user\"=? RETURNING *")
        sqlv [sql id user name data]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (decode-data))))

(defmethod usc/-novelty :update/color-collection
  [conn params]
  (->> (usc/validate! params update-color-coll-schema)
       (update-color-coll-schema conn)))

;; --- Delete Color Collection

(def ^:private delete-color-coll-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-color-collection
  [conn {:keys [id user]}]
  (let [sql (str "DELETE FROM color_collections "
                 " WHERE id=? AND \"user\"=?")]
    (pos? (sc/execute conn [sql id user]))))

(defmethod usc/-novelty :delete/color-collection
  [conn params]
  (->> (usc/validate! params delete-color-coll-schema)
       (delete-color-collection conn)))

;; --- List Projects

(defn get-color-collections-for-user
  [conn user]
  (let [sql (str "SELECT * FROM color_collections "
                 " WHERE \"user\"=? ORDER BY created_at DESC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs)
         (map decode-data))))

(defmethod usc/-query :list/color-collections
  [conn {:keys [user] :as params}]
  (get-color-collections-for-user conn user))

;; --- Helpers

(defn- decode-data
  [{:keys [data] :as result}]
  (let [data (some-> data
                     (codecs/str->bytes)
                     (t/decode))]
    (assoc result :data data)))
