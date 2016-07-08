;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.pages
  (:require [suricatta.core :as sc]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.db :as db]
            [uxbox.services.core :as usc]
            [uxbox.services.locks :as locks]
            [uxbox.services.auth :as usauth]
            [uxbox.util.time :as dt]
            [uxbox.util.data :as data]
            [uxbox.util.transit :as t]
            [uxbox.util.blob :as blob]
            [uxbox.util.uuid :as uuid]))

(def validate! (partial us/validate! :service/wrong-arguments))

(declare decode-page-data)
(declare decode-page-options)
(declare encode-data)

;; --- Create Page

(def ^:private create-page-schema
  {:id [us/uuid]
   :data [us/required us/string]
   :options [us/required us/string]
   :user [us/required us/uuid]
   :project [us/required us/uuid]
   :name [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(defn create-page
  [conn {:keys [id user project name width
                height layout data options] :as params}]
  (let [opts {:id (or id (uuid/random))
              :user user
              :project project
              :name name
              :width width
              :height height
              :layout layout
              :data (blob/encode data)
              :options (blob/encode options)}
        sqlv (sql/create-page opts)]
    (->> (sc/fetch-one conn sqlv)
         (data/normalize-attrs)
         (decode-page-data)
         (decode-page-options))))

(defmethod usc/-novelty :create/page
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params create-page-schema)
         (create-page conn))))

;; --- Update Page

(def ^:private update-page-schema
  (assoc create-page-schema
         :version [us/required us/number]))

(defn update-page
  [conn {:keys [id user project name width height
                layout data version options] :as params}]
  (let [opts {:id (or id (uuid/random))
              :user user
              :project project
              :name name
              :width width
              :version version
              :height height
              :layout layout
              :data (blob/encode data)
              :options (blob/encode options)}
        sqlv (sql/update-page opts)]
    (some-> (sc/fetch-one conn sqlv)
            (data/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))

(defmethod usc/-novelty :update/page
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-page-schema)
         (update-page conn))))

;; --- Update Page Metadata

(def ^:private update-page-metadata-schema
  (dissoc update-page-schema :data))

(defn update-page-metadata
  [conn {:keys [id user project name width
                height layout version options] :as params}]
  (let [opts {:id (or id (uuid/random))
              :user user
              :project project
              :name name
              :width width
              :version version
              :height height
              :layout layout
              :options (blob/encode options)}
        sqlv (sql/update-page-metadata opts)]
    (some-> (sc/fetch-one conn sqlv)
            (data/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))

(defmethod usc/-novelty :update/page-metadata
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-page-metadata-schema)
         (update-page-metadata conn))))

;; --- Delete Page

(def ^:private delete-page-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-page
  [conn {:keys [id user] :as params}]
  (let [sqlv (sql/delete-page {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(defmethod usc/-novelty :delete/page
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params delete-page-schema)
         (delete-page conn))))

;; --- List Pages by Project

(def ^:private list-pages-by-project-schema
  {:user [us/required us/uuid]
   :project [us/required us/uuid]})

(defn get-pages-for-project
  [conn project]
  (let [sqlv (sql/get-pages-for-project {:project project})]
    (->> (sc/fetch conn sqlv)
         (map data/normalize-attrs)
         (map decode-page-data)
         (map decode-page-options))))

(defn get-pages-for-user-and-project
  [conn {:keys [user project]}]
  (let [sqlv (sql/get-pages-for-user-and-project
              {:user user :project project})]
    (->> (sc/fetch conn sqlv)
         (map data/normalize-attrs)
         (map decode-page-data)
         (map decode-page-options))))

(defmethod usc/-query :list/pages-by-project
  [{:keys [user project] :as params}]
  (with-open [conn (db/connection)]
    (->> (validate! params list-pages-by-project-schema)
         (get-pages-for-user-and-project conn))))

;; --- Page History (Query)

(def ^:private list-page-history-schema
  {:user [us/required us/uuid]
   :id [us/required us/uuid]
   :max [us/integer]
   :pinned [us/boolean]
   :since [us/integer]})

(defn get-page-history
  [conn {:keys [id user since max pinned]
         :or {since Long/MAX_VALUE
              max 10}}]
  (let [sqlv (sql/get-page-history {:user user
                                    :page id
                                    :since since
                                    :max max
                                    :pinned pinned})]
    (->> (sc/fetch conn sqlv)
         (map data/normalize-attrs)
         (map decode-page-data))))

(defmethod usc/-query :list/page-history
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params list-page-history-schema)
         (get-page-history conn))))

;; --- Update Page History

(def ^:private update-page-history-schema
  {:user [us/required us/uuid]
   :id [us/required us/uuid]
   :label [us/required us/string]
   :pinned [us/required us/boolean]})

(defn update-page-history
  [conn {:keys [user id label pinned]}]
  (let [sqlv (sql/update-page-history {:user user
                                       :id id
                                       :label label
                                       :pinned pinned})]
    (some-> (sc/fetch-one conn sqlv)
            (data/normalize-attrs)
            (decode-page-data))))

(defmethod usc/-novelty :update/page-history
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-page-history-schema)
         (update-page-history conn))))

;; --- Helpers

(defn- decode-page-options
  [{:keys [options] :as result}]
  (merge result (when options
                  {:options (blob/decode->str options)})))

(defn- decode-page-data
  [{:keys [data] :as result}]
  (merge result (when data
                  {:data (blob/decode->str data)})))

(defn get-page-by-id
  [conn id]
  (let [sqlv (sql/get-page-by-id {:id id})]
    (some-> (sc/fetch-one conn sqlv)
            (data/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))
