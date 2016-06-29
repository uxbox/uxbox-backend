;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.projects
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(def validate-form! (partial us/validate! :form/validation))

;; --- Get Project (By ID)

(defn retrieve-project
  [{params :route-params}]
  (let [message {:type :retrieve/project-by-id
                 :id (uuid/from-string (:id params))}]
    (->> (sv/query message)
         (p/map (fn [v]
                  (if v
                    (http/ok (rsp v))
                    (http/not-found "")))))))

;; --- List Projects

(defn list-projects
  [{user :identity}]
  (let [message {:user user :type :list/projects}]
    (->> (sv/query message)
         (p/map #(http/ok (rsp %))))))

;; --- Create Projects

(def ^:private create-project-schema
  {:id [us/uuid] :name [us/required us/string]})

(defn create-project
  [{user :identity data :data}]
  (let [data (validate-form! data create-project-schema)
        message (assoc data
                       :type :create/project
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/projects/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Project

(def ^:private update-project-schema
  {:id [us/uuid]
   :name [us/required us/string]
   :version [us/required us/integer]})

(defn update-project
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-project-schema)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/project
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Project

(defn delete-project
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/project
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))
