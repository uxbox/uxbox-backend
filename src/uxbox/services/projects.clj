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
            [uxbox.services.core :as usc]
            [uxbox.util.transit :as t]))

(def validate! (partial us/validate! :service/wrong-arguments))

;; --- Create Project

(def ^:private create-project-schema
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]})

(defn create-project
  [conn {:keys [id user name] :as data}]
  (let [sql (str "INSERT INTO projects (id, \"user\", name)"
                 " VALUES (?, ?, ?) RETURNING *")
        id (or id (uuid/v4))
        sqlv [sql id user name]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defmethod usc/-novelty :create/project
  [conn params]
  (->> (validate! params create-project-schema)
       (create-project conn)))

;; --- Update Project

(def ^:private update-project-schema
  (assoc create-project-schema
         :version [us/required us/integer]))

(defn- update-project
  [conn {:keys [name version id user] :as data}]
  (let [sql (str "UPDATE projects SET name=?, version=?"
                 " WHERE id=? AND \"user\"=? AND deleted=false "
                 " RETURNING *")
        sqlv [sql name version id user]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defmethod usc/-novelty :update/project
  [conn params]
  (->> (validate! params update-project-schema)
       (update-project conn)))

;; --- Delete Project

(def ^:private delete-project-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn- delete-project
  [conn {:keys [id user] :as data}]
  (let [sql (str "UPDATE projects "
                 " SET deleted=true, deleted_at=clock_timestamp() "
                 " WHERE id=? AND \"user\"=? AND deleted=false")]
    (pos? (sc/execute conn [sql id user]))))

(defmethod usc/-novelty :delete/project
  [conn params]
  (->> (validate! params delete-project-schema)
       (delete-project conn)))

;; --- List Projects

(defn get-projects-for-user
  [conn user]
  (let [sql (str "SELECT pr.*, count(pg.id) as total_pages "
                 " FROM projects AS pr "
                 " LEFT OUTER JOIN pages AS pg "
                 " ON pg.project=pr.id "
                 " WHERE pr.user=? AND pr.deleted = false "
                 " GROUP BY pr.id "
                 " ORDER BY pr.created_at DESC")]
    (->> (sc/fetch conn [sql user])
         (map usc/normalize-attrs))))

(defmethod usc/-query :list/projects
  [conn {:keys [user] :as params}]
  (get-projects-for-user conn user))

;; --- Helpers

(defn get-project-by-id
  [conn id]
  (let [sqlv ["SELECT * FROM projects WHERE id=? AND deleted=false" id]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))
