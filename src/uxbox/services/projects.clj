;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.projects
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

;; Create Project

(def +create-project-schema+
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]})

(defn create-project
  [conn {:keys [id user name] :as data}]
  (us/validate! data +create-project-schema+)
  (let [sql (str "INSERT INTO projects (id, \"user\", name)"
                 " VALUES (?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user name]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;; Update Project

(def +update-project-schema+
  (assoc +create-project-schema+
         :version [us/required us/number]))

(defn update-project
  [conn {:keys [id user name version] :as data}]
  (us/validate! data +update-project-schema+)
  (let [sql (str "UPDATE projects SET name=?, version=?"
                 " WHERE id=? AND \"user\"=? RETURNING *")
        sqlv [sql name version id user]]
    (locks/acquire! conn id)
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(def +delete-project-schema+
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn delete-project
  [conn {:keys [id user] :as params}]
  (let [sql "DELETE FROM projects WHERE id=?::uuid AND \"user\"=?::uuid"]
    (sc/execute conn [sql id user])
    nil))

;; Query Projects

(defn get-projects-for-user
  [conn user]
  (let [sql (str "SELECT * FROM projects "
                 " WHERE \"user\"=? ORDER BY created_at DESC")]
    (map usc/normalize-attrs
         (sc/fetch conn [sql user]))))

(defn get-project-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM projects WHERE id=?" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service (novelty)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod usc/-novelty :project/create
  [conn params]
  (create-project conn params))

(defmethod usc/-novelty :project/update
  [conn params]
  (update-project conn params))

(defmethod usc/-novelty :project/delete
  [conn params]
  (delete-project conn params))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service (query)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod usc/-query :project/list
  [conn {:keys [user] :as params}]
  (get-projects-for-user conn user))

