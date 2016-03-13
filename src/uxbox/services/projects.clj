;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.projects
  (:require [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [catacumba.serializers :as sz]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]
            [uxbox.services.locks :as locks]
            [uxbox.services.auth :as usauth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +create-project-schema+
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]})

(def +update-project-schema+
  (assoc +create-project-schema+
         :version [us/required us/number]))

(def +create-page-schema+
  {:id [us/uuid]
   :user [us/required us/uuid]
   :project [us/required us/uuid]
   :name [us/required us/string]
   :data [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(def +update-page-schema+
  (assoc +update-project-schema+
         :version [us/required us/number]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repository
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-project
  [conn {:keys [id user name] :as data}]
  {:pre [(us/validate! data +create-project-schema+)]}
  (let [sql (str "INSERT INTO projects (id, \"user\", name)"
                 " VALUES (?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user name]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn update-project
  [conn {:keys [id user name version] :as data}]
  {:pre [(us/validate! data +update-project-schema+)]}
  (let [sql (str "UPDATE projects SET name=?, version=?"
                 " WHERE id=? AND \"user\"=? RETURNING *")
        sqlv [sql name version id user]]
    (locks/acquire! conn id)
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn delete-project
  [conn {:keys [id user] :as params}]
  (let [sql "DELETE FROM projects WHERE id=? AND \"user\"=?"]
    (sc/execute conn [sql id user])
    nil))

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

(defn delete-page
  [conn {:keys [id user] :as params}]
  (let [sql "DELETE FROM pages WHERE id=? AND \"user\"=?"]
    (sc/execute conn [sql id user])
    nil))

(defn get-projects-for-user
  [conn user]
  (let [sql (str "SELECT * FROM projects "
                 " WHERE \"user\"=? ORDER BY created_at DESC")]
    (map usc/normalize-attrs
         (sc/fetch conn [sql user]))))

(defn get-pages-for-user
  [conn user]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=?"
                 " ORDER BY created_at DESC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs))))

(defn get-pages-for-project-and-user
  [conn user project]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE \"user\"=? AND project=? "
                 " ORDER BY created_at DESC")]
    (->> (sc/fetch conn [sql user project])
         (map usc/normalize-attrs))))

(defn get-project-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM projects WHERE id=?" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn get-page-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM pages WHERE id=?" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- decode-page-data
  [{:keys [data] :as result}]
  (let [data (some-> data
                     (codecs/base64->bytes)
                     (sz/decode :transit+msgpack))]
    (assoc result :data data)))

(defmethod usc/-novelty :project/create
  [conn params]
  (create-project conn params))

(defmethod usc/-novelty :project/update
  [conn params]
  (update-project conn params))

(defmethod usc/-novelty :project/delete
  [conn params]
  (delete-project conn params))

(defmethod usc/-novelty :page/create
  [conn {:keys [data] :as params}]
  (let [data (-> (sz/encode data :transit+msgpack)
                 (codecs/bytes->base64))
        params (assoc params :data data)]
    (-> (create-page conn params)
        (decode-page-data))))

(defmethod usc/-novelty :page/update
  [conn {:keys [data] :as params}]
  (let [data (-> (sz/encode data :transit+msgpack)
                 (codecs/bytes->base64))
        params (assoc params :data data)]
    (-> (update-page conn params)
        (decode-page-data))))

(defmethod usc/-novelty :page/delete
  [conn params]
  (delete-page conn params))

(defmethod usc/-query :project/list
  [conn {:keys [user] :as params}]
  (get-projects-for-user conn user))

(defmethod usc/-query :page/list
  [conn {:keys [user] :as params}]
  (->> (get-pages-for-user conn user)
       (map decode-page-data)))
