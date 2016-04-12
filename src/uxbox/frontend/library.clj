;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.library
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(def validate-form! (partial us/validate! :form/validation))

;; --- List Color Collections

(defn list-color-collections
  [{user :identity}]
  (let [message {:user user
                :type :list/color-collections}]
    (->> (sv/query message)
         (p/map #(http/ok (rsp %))))))

;; --- Create Color Collection

(def ^:private create-color-coll-schema
  {:id [us/uuid]
   :name [us/required us/string]
   :data [us/required us/set]})

(defn create-color-collection
  [{user :identity data :data}]
  (let [params (validate-form! data create-color-coll-schema)
        message (assoc params
                       :type :create/color-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/library/colors/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Color Collection

(def ^:private update-color-coll-schema
  (assoc create-color-coll-schema
         :version [us/required us/integer]))

(defn update-color-collection
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-color-coll-schema)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/color-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))

;; --- Delete Color Collection

(defn delete-color-collection
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/color-collection
                 :user user}]
    (->> (sv/novelty message)
         (p/map (fn [v] (http/no-content))))))
