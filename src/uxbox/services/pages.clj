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
            [uxbox.util.transit :as t]
            [uxbox.services.core :as usc]
            [uxbox.services.locks :as locks]
            [uxbox.services.auth :as usauth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repository
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Create Page

(def ^:private +create-page-schema+
  {:id [us/uuid]
   :user [us/required us/uuid]
   :project [us/required us/uuid]
   :name [us/required us/string]
   :data [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(defn create-page
  [conn {:keys [id user project name width height layout data] :as params}]
  {:pre [(us/validate! params +create-page-schema+)]}
  (let [sql (str "INSERT INTO pages (id, \"user\", project, name, width, "
                 "                   height, layout, data)"
                 " VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user project name width height layout data]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;; Update Page

(def ^:private +update-page-schema+
  (assoc +create-page-schema+
         :version [us/required us/number]))

(defn update-page
  [conn {:keys [id user project name width height layout data version] :as params}]
  {:pre [(us/validate! params +update-page-schema+)]}
  (let [sql (str "UPDATE pages SET "
                 " name=?, width=?, height=?, layout=?, data=?, version=?"
                 " WHERE id=? AND \"user\"=? AND project=?"
                 " RETURNING *")
        sqlv [sql name width height layout data version id user project]]
    (locks/acquire! conn id)
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;; Update Page Metadata

(def ^:private +update-page-metadata-schema+
  (dissoc +update-page-schema+ :data))

(defn update-page-metadata
  [conn {:keys [id user project name width height layout version] :as params}]
  {:pre [(us/validate! params +update-page-metadata-schema+)]}
  (let [sql (str "UPDATE pages SET "
                 " name=?, width=?, height=?, layout=?, version=?"
                 " WHERE id=? AND \"user\"=? AND project=?"
                 " RETURNING *")
        sqlv [sql name width height layout version id user project]]
    (locks/acquire! conn id)
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;; Delete Page

(def +delete-page-schema+
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-page
  [conn {:keys [id user] :as params}]
  {:pre [(us/validate! params +delete-page-schema+)]}
  (let [sql "DELETE FROM pages WHERE id=? AND \"user\"=?"]
    (sc/execute conn [sql id user])
    nil))

;; Query Pages

(defn get-pages-for-user
  [conn user]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=?"
                 " ORDER BY created_at ASC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs))))

(defn get-pages-for-user-and-project
  [conn user project]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=? AND project=? "
                 " ORDER BY created_at ASC")]
    (->> (sc/fetch conn [sql user project])
         (map usc/normalize-attrs))))

(defn get-page-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM pages WHERE id=?" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn get-page-history
  [conn {:keys [id user since max] :as {since (dt/now) max 10}}]
  (let [sql (str "SELECT * FROM pages_history "
                 " WHERE \"user\"=? AND id=?"
                 " AND created_at < ?")
        sqlv [sql user id since]]
    (->> (sc/fetch conn [sql user project])
         (map usc/normalize-attrs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service (novelty)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- decode-page-data
  [{:keys [data] :as result}]
  (let [data (some-> data
                     (codecs/str->bytes)
                     (t/decode))]
    (assoc result :data data)))

(defmethod usc/-novelty :page/create
  [conn {:keys [data] :as params}]
  (let [data (-> (t/encode data)
                 (codecs/bytes->str))
        params (assoc params :data data)]
    (-> (create-page conn params)
        (decode-page-data))))

(defmethod usc/-novelty :page/update
  [conn {:keys [data] :as params}]
  (let [data (-> (t/encode data)
                 (codecs/bytes->str))
        params (assoc params :data data)]
    (-> (update-page conn params)
        (decode-page-data))))

(defmethod usc/-novelty :page/update-metadata
  [conn {:keys [data] :as params}]
  (let [data (-> (t/encode data)
                 (codecs/bytes->str))
        params (assoc params :data data)]
    (-> (update-page-metadata conn params)
        (decode-page-data))))

(defmethod usc/-novelty :page/delete
  [conn params]
  (delete-page conn params))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service (query)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod usc/-query :page/list
  [conn {:keys [user] :as params}]
  (->> (get-pages-for-user conn user)
       (map decode-page-data)))

(def +query-page-history-list-schema+
  {:user [us/required us/uuid]
   :id [us/required us/uuid]
   :max [us/number]
   :since [us/datetime]})

(defmethod usc/-query :page/history-list
  [conn {:keys [id user since] :as params}]
  {:pre [(us/validate! params +query-page-history-list-schema+)]}
  (->> (get-page-history conn params)
       (map decode-page-data)))

(def +query-page-list-by-project-schema+
  {:user [us/required us/uuid]
   :project [us/required us/uuid]})

(defmethod usc/-query :page/list-by-project
  [conn {:keys [user project] :as params}]
  {:pre [(us/validate! params +query-page-list-by-project-schema+)]}
  (->> (get-pages-for-user-and-project conn user project)
       (map decode-page-data)))
