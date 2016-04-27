;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.images
  "Images library related services."
  (:require [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.util.transit :as t]
            [uxbox.services.core :as usc]))

(def validate! (partial us/validate! :service/wrong-arguments))

;; --- Create Collection

(defn create-collection
  [conn {:keys [id user name]}]
  (let [sql (str "INSERT INTO image_collections (id, \"user\", name) "
                 " VALUES (?, ?, ?) RETURNING *")
        id (or id (uuid/v4))]
    (-> (sc/fetch-one conn [sql id user name])
        (usc/normalize-attrs))))

(def ^:private create-collection-scheme
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]})

(defmethod usc/-novelty :create/image-collection
  [conn params]
  (->> (validate! params create-collection-scheme)
       (create-collection conn)))

;; --- Update Collection

(defn update-collection
  [conn {:keys [id user name version]}]
  (let [sql (str "UPDATE image_collections SET name=?,version=? "
                 " WHERE id=? AND \"user\"=? RETURNING *")
        sqlv [sql id user name data]]
    (some-> (sc/fetch-one conn [sql name version id user])
            (usc/normalize-attrs)
            (decode-data))))

(def ^:private update-collection-scheme
  (assoc create-collection-scheme
         :version [us/required us/integer]))

(defmethod usc/-novelty :update/image-collection
  [conn params]
  (->> (validate! params update-collection-scheme)
       (update-collection conn)))

;; --- List Collections

(defn get-collections-by-user
  [conn user]
  (let [sql (str "SELECT * FROM image_collections "
                 " WHERE \"user\"=? ORDER BY created_at DESC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs))))

(defmethod usc/-query :list/image-collections
  [conn {:keys [user] :as params}]
  (get-collections-by-user conn user))

;; --- Delete Collection

(def ^:private delete-collection-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-collection
  [conn {:keys [id user]}]
  (let [sql (str "DELETE FROM image_collections "
                 " WHERE id=? AND \"user\"=?")]
    (pos? (sc/execute conn [sql id user]))))

(defmethod usc/-novelty :delete/color-collection
  [conn params]
  (->> (validate! params delete-collection-schema)
       (delete-collection conn)))
