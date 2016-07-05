;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.projects
  (:require [suricatta.core :as sc]
            [buddy.core.codecs :as codecs]
            [uxbox.config :as ucfg]
            [uxbox.schema :as us]
            [uxbox.sql :as sql]
            [uxbox.db :as db]
            [uxbox.services.core :as usc]
            [uxbox.util.transit :as t]
            [uxbox.util.uuid :as uuid]))

(def validate! (partial us/validate! :service/wrong-arguments))

;; --- Create Project

(def ^:private create-project-schema
  {:id [us/uuid]
   :user [us/required us/uuid]
   :name [us/required us/string]})

(defn create-project
  [conn {:keys [id user name] :as data}]
  (let [id (or id (uuid/random))
        sqlv (sql/create-project {:id id :user user :name name})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defmethod usc/-novelty :create/project
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params create-project-schema)
         (create-project conn))))

;; --- Update Project

(def ^:private update-project-schema
  (assoc create-project-schema
         :version [us/required us/integer]))

(defn- update-project
  [conn {:keys [name version id user] :as data}]
  (let [sqlv (sql/update-project {:name name
                                  :version version
                                  :id id
                                  :user user})]
    (some-> (sc/fetch-one conn sqlv)
            (usc/normalize-attrs))))

(defmethod usc/-novelty :update/project
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params update-project-schema)
         (update-project conn))))

;; --- Delete Project

(def ^:private delete-project-schema
  {:id [us/required us/uuid]
   :user [us/required us/uuid]})

(defn- delete-project
  [conn {:keys [id user] :as data}]
  (let [sqlv (sql/delete-project {:id id :user user})]
    (pos? (sc/execute conn sqlv))))

(defmethod usc/-novelty :delete/project
  [params]
  (with-open [conn (db/connection)]
    (->> (validate! params delete-project-schema)
         (delete-project conn))))

;; --- List Projects

(defn get-projects-for-user
  [conn user]
  (let [sqlv (sql/get-projects {:user user})]
    (->> (sc/fetch conn sqlv)
         (map usc/normalize-attrs))))

(defmethod usc/-query :list/projects
  [{:keys [user] :as params}]
  (with-open [conn (db/connection)]
    (get-projects-for-user conn user)))
