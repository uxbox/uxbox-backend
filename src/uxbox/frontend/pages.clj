;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.pages
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

(def validate-form! (partial us/validate! :form/validation))
(def validate-query! (partial us/validate! :query/validation))

;; --- List Pages

(defn list-pages-by-project
  [{user :identity params :route-params}]
  (let [params {:user user
                :project (uuid/from-string (:id params))
                :type :list/pages-by-project}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

;; --- Create Page

(def ^:private create-page-schema
  {:id [us/uuid]
   :data [us/required us/string]
   :options [us/required us/string]
   :project [us/required us/uuid]
   :name [us/required us/string]
   :width [us/required us/integer]
   :height [us/required us/integer]
   :layout [us/required us/string]})

(defn create-page
  [{user :identity data :data}]
  (let [data (validate-form! data create-page-schema)
        message (assoc data
                       :type :create/page
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/pages/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Page

(def ^:private update-page-schema
  (assoc create-page-schema
         :version [us/required us/number]))

(defn update-page
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-page-schema)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/page
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))


;; --- Update Page Metadata

(def ^:private update-page-metadata-schema
  (dissoc update-page-schema :data))

(defn update-page-metadata
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-page-metadata-schema)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/page-metadata
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Page

(defn delete-page
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/page
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))

;; --- Retrieve Page History

(def ^:private retrieve-page-history-query-schema
  {:max [us/integer-str [us/in-range 0 Long/MAX_VALUE]]
   :since [us/integer-str]
   :pinned [us/boolean-str]})

(defn retrieve-page-history
  [{user :identity params :route-params query :query-params}]
  (let [query (validate-query! query retrieve-page-history-query-schema)
        message (assoc query
                       :id (uuid/from-string (:id params))
                       :type :list/page-history
                       :user user)]
    (->> (sv/query message)
         (p/map #(http/ok (rsp %))))))

;; --- Update Page History

(def ^:private update-page-history-schema
  {:label [us/required us/string]
   :pinned [us/required us/boolean]})

(defn update-page-history
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-page-history-schema)
        message (assoc data
                       :type :update/page-history
                       :id (uuid/from-string (:hid params))
                       :user user)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))
