;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.projects
  (:require [mount.core :as mount :refer (defstate)]
            [suricatta.core :as sc]
            [buddy.hashers :as hashers]
            [buddy.sign.jwe :as jwe]
            [buddy.core.nonce :as nonce]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]
            [uxbox.services.auth :as usauth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +project-schema+
  {:id [us/required us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(def ^:const +page-schema+
  (assoc +project-schema+
         :project [us/required us/uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repository
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: maybe move sql into files?

(defn create-project
  [conn {:keys [id user name width height layout] :as data}]
  {:pre [(us/validate! data +project-schema+)]}
  (let [sql (str "INSERT INTO projects (id, user, name, width, height, layout)"
                 " VALUES (?, ?, ?, ?, ?) RETURNING *")
        sqlv [sql id user name width height layout]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn create-page
  [conn {:keys [id user project name width height layout] :as data}]
  {:pre [(us/validate! data +page-schema+)]}
  (let [sql (str "INSERT INTO pages (id, user, project, name, width, "
                 "                   height, layout)"
                 " VALUES (?, ?, ?, ?, ?, ?) RETURNING *")
        sqlv [sql id user project name width height layout]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn update-project
  [conn {:keys [id user name width height layout] :as data}]
  {:pre [(us/validate! data +project-schema+)]}
  (let [sql (str "UPDATE projects SET "
                 " name=?, width=? height=?, layout=?,"
                 " modified_at=current_timestamp "
                 " WHERE id=? AND user=? RETURNING *")
        sqlv [sql name width height layout id user]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn update-page
  [conn {:keys [id user project name width height layout] :as data}]
  {:pre [(us/validate! data +page-schema+)]}
  (let [sql (str "UPDATE projects SET "
                 " name=?, width=? height=?, layout=?,"
                 " modified_at=current_timestamp"
                 " WHERE id=? AND user=? AND project=?"
                 " RETURNING *")
        sqlv [sql name width height layout id user project]]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defn get-projects-for-user
  [conn user]
  (let [sql (str "SELECT * FROM projects "
                 " WHERE user=? ORDER BY created_at DESC")]
    (map usc/normalize-attrs
         (sc/fetch conn [sql user]))))

(defn get-pages-for-project-and-user
  [conn user project]
  (let [sql (str "SELECT * FROM pages "
                 " WHERE user=? AND project=? "
                 " ORDER BY created_at DESC")]
    (map usc/normalize-attrs
         (sc/fetch conn [sql user project]))))

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

(defmethod usc/-query :project/list
  [conn {:keys [user]}]
  (get-projects-for-user conn user))

(defmethod usc/-query :page/list
  [conn {:keys [user project]}]
  (get-pages-for-project-and-user conn user project))

(defmethod usc/-novelty :project/create
  [conn {:keys []}]
  )
