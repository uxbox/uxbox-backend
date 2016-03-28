;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.pages
  (:require [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.persistence :as up]
            [uxbox.util.time :as dt]
            [uxbox.util.transit :as t]
            [uxbox.services.core :as usc]
            [uxbox.services.locks :as locks]
            [uxbox.services.auth :as usauth]))

(declare decode-page-data)
(declare decode-page-options)

;; --- Create Page

(def ^:private create-page-schema
  {:id [us/uuid]
   :data [us/coll]
   :options [us/coll]
   :user [us/required us/uuid]
   :project [us/required us/uuid]
   :name [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(defn create-page
  [conn {:keys [id user project name width
                height layout data options] :as params}]
  (let [data (codecs/bytes->str (t/encode data))
        options (codecs/bytes->str (t/encode options))
        sql (str "INSERT INTO pages (id, \"user\", project, name, width, "
                 "                   height, layout, data, options)"
                 " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user project name width
              height layout data options]]
    (->> (sc/fetch-one conn sqlv)
         (usc/normalize-attrs)
         (decode-page-data)
         (decode-page-options))))

(defmethod usc/-novelty :create/page
  [conn {:keys [data] :as params}]
  (->> (usc/validate! params create-page-schema)
       (create-page conn)))

;; --- Update Page

(def ^:private update-page-schema
  (assoc create-page-schema
         :version [us/required us/number]))

(defn update-page
  [conn {:keys [id user project name width height
                layout data version options] :as params}]
  (let [data (codecs/bytes->str (t/encode data))
        options (codecs/bytes->str (t/encode options))
        sql (str "UPDATE pages SET name=?, width=?, height=?, layout=?, "
                 "                 data=?, version=?, options=? "
                 " WHERE id=? AND \"user\"=? AND project=?"
                 " RETURNING *")
        sqlv [sql name width height layout data
              version options id user project]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))

(defmethod usc/-novelty :update/page
  [conn {:keys [data] :as params}]
  (->> (usc/validate! params update-page-schema)
       (update-page conn)))

;; --- Update Page Metadata

(def ^:private update-page-metadata-schema
  (dissoc update-page-schema :data))

(defn update-page-metadata
  [conn {:keys [id user project name width
                height layout version options] :as params}]
  (let [options (codecs/bytes->str (t/encode options))
        sql (str "UPDATE pages SET name=?, width=?, height=?, layout=?, "
                 "                 version=?, options=? "
                 " WHERE id=? AND \"user\"=? AND project=?"
                 " RETURNING *")
        sqlv [sql name width height layout
              version options id user project]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))

(defmethod usc/-novelty :update/page-metadata
  [conn params]
  (->> (usc/validate! params update-page-metadata-schema)
       (update-page-metadata conn)))

;; --- Delete Page

(def ^:private delete-page-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-page
  [conn {:keys [id user] :as params}]
  (let [sql "DELETE FROM pages WHERE id=? AND \"user\"=?"]
    (sc/execute conn [sql id user])
    nil))

(defmethod usc/-novelty :delete/page
  [conn params]
  (usc/validate! params delete-page-schema)
  (delete-page conn params))

;; --- List Pages

;; TODO: consider using transducers

(defn get-pages-for-user
  [conn user]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=?"
                 " ORDER BY created_at ASC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs)
         (map decode-page-data)
         (map decode-page-options))))

(defmethod usc/-query :list/pages
  [conn {:keys [user] :as params}]
  (get-pages-for-user conn user))

;; --- List Pages by Project

(def ^:private list-pages-by-project-schema
  {:user [us/required us/uuid]
   :project [us/required us/uuid]})

(defn get-pages-for-user-and-project
  [conn user project]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=? AND project=? "
                 " ORDER BY created_at ASC")]
    (->> (sc/fetch conn [sql user project])
         (map usc/normalize-attrs)
         (map decode-page-data)
         (map decode-page-options))))

(defmethod usc/-query :list/pages-by-project
  [conn {:keys [user project] :as params}]
  (usc/validate! params list-pages-by-project-schema)
  (get-pages-for-user-and-project conn user project))

;; --- Page History (Query)

(def ^:private list-page-history-schema
  {:user [us/required us/uuid]
   :id [us/required us/uuid]
   :max [us/integer]
   :pinned [us/boolean]
   :since [us/integer]})

(defn get-page-history
  [conn {:keys [id user since max pinned] :or {since Long/MAX_VALUE
                                               max Long/MAX_VALUE}}]
  (let [sql (str "SELECT * FROM pages_history "
                 " WHERE \"user\"=? AND page=? AND version < ?"
                 (if pinned " AND pinned = true " "")
                 " ORDER BY version DESC"
                 " LIMIT ?")
        sqlv [sql user id since max]]
    (->> (sc/fetch conn sqlv)
         (map usc/normalize-attrs)
         (map decode-page-data))))

(defmethod usc/-query :list/page-history
  [conn params]
  (->> (usc/validate! params list-page-history-schema)
       (get-page-history conn)))

;; --- Update Page History

(def ^:private update-page-history-schema
  {:user [us/required us/uuid]
   :id [us/required us/uuid]
   :label [us/required us/string]
   :pinned [us/required us/boolean]})

(defn update-page-history
  [conn {:keys [user id label pinned]}]
  (let [sql (str "UPDATE pages_history SET "
                 " label=?, pinned=? "
                 " WHERE id=? AND \"user\"=? "
                 " RETURNING *")]
    (some-> (sc/fetch-one conn [sql label pinned id user])
            (usc/normalize-attrs)
            (decode-page-data))))

(defmethod usc/-novelty :update/page-history
  [conn params]
  (->> (usc/validate! params update-page-history-schema)
       (update-page-history conn)))

;; --- Helpers

(defn- decode-page-options
  [{:keys [options] :as result}]
  (let [options (some-> options
                        (codecs/str->bytes)
                        (t/decode))]
    (assoc result :options options)))

(defn- decode-page-data
  [{:keys [data] :as result}]
  (let [data (some-> data
                     (codecs/str->bytes)
                     (t/decode))]
    (assoc result :data data)))

(defn get-page-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM pages WHERE id=?" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs)
            (decode-page-data)
            (decode-page-options))))


