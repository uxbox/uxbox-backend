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
            [uxbox.sql :as sql]
            [uxbox.util.transit :as t]
            [uxbox.services.core :as usc])
  (:import ratpack.form.UploadedFile
           org.apache.commons.io.FilenameUtils))

(def validate! (partial us/validate! :service/wrong-arguments))

;; --- Create Collection

(defn create-collection
  [conn {:keys [id user name]}]
  (let [id (or id (uuid/v4))
        sqlv (sql/create-image-collection {:id id
                                           :user user
                                           :name name})]
    (-> (sc/fetch-one conn sqlv)
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
  (let [sqlv (sql/update-image-collection {:id id
                                           :user user
                                           :name name
                                           :version version})]
    (some-> (sc/fetch-one conn sqlv)
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
  (let [sqlv (sql/get-image-collections {:user user})]
    (->> (sc/fetch conn sqlv)
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
  (let [sqlv (sql/delete-image-collection {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(defmethod usc/-novelty :delete/image-collection
  [conn params]
  (->> (validate! params delete-collection-schema)
       (delete-collection conn)))

;; --- Create Image (Upload)

(defn create-image
  [conn {:keys [id user file collection]}]
  (let [id (or id (uuid/v4))
        filename (.getFileName ^UploadedFile file)
        ext (FilenameUtils/getExtension oname)
        path @(st/save (str id "." ext) file)
        sqlv (sql/create-image {:id id
                                :path path
                                :collection collection
                                :user user})]
    (->> (sc/fetch conn sqlv)
         (map usc/normalize-attrs))))

(def ^:private create-image-schema
  {:id [us/uuid]
   :user [us/required us/uuid]
   :file [us/required us/uploaded-file]})

(defmethod usc/-novelty :create/image
  [conn params]
  (->> (validate! params create-image-schema)
       (create-image conn)))

;; --- Update Image.

(defn update-image
  [conn {:keys [id name version]}]

;; --- Delete Image.

(defn delete-image
  [conn {:keys [user id]}]
  (let [sqlv (sql/delete-image {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(def ^:private delete-image-schema
  {:id [us/uuid]
   :user [us/required us/uuid]})

(defmethod usc/-novelty :delete/image
  [conn params]
  (->> (validate! params delete-image-schema)
       (delete-image conn)))
