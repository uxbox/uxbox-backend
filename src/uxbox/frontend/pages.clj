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
            [uxbox.frontend.core :refer (validate!)]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

;; --- List Pages

(defn list-pages
  [{user :identity}]
  (let [params {:user user :type :list/pages}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

(defn list-pages-by-project
  [{user :identity params :route-params}]
  (let [params {:user user
                :project (uuid/from-string (:id params))
                :type :list/pages-by-project}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

;; --- Create Page

;; TODO: add validations

(defn create-page
  [{user :identity params :data}]
  (p/alet [params (assoc params
                         :type :create/page
                         :user user)
           result (p/await (sv/novelty params))
           loc (str "/api/pages/" (:id result))]
    (http/created loc (rsp result))))

;; --- Update Page

;; TODO: add validations

(defn update-page
  [{user :identity params :route-params data :data}]
  (let [params (assoc data
                      :id (uuid/from-string (:id params))
                      :type :update/page
                      :user user)]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

(defn update-page-metadata
  [{user :identity params :route-params data :data}]
  (let [params (merge data
                      {:id (uuid/from-string (:id params))
                       :type :update/page-metadata
                       :user user})]
    (-> (sv/novelty params)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Page

(defn delete-page
  [{user :identity params :route-params}]
  (let [params {:id (uuid/from-string (:id params))
                :type :delete/page
                :user user}]
    (-> (sv/novelty params)
        (p/then (fn [v] (http/no-content))))))

;; --- Retrieve Page History

(def retrieve-page-history-query-schema
  {:max [us/integer-like [us/in-range 0 Long/MAX_VALUE]]
   :since [us/integer-like us/positive]
   :pinned [us/boolean-like]})

(def retrieve-page-history-params-schema
  {:id [us/required us/uuid-like]})

(defn retrieve-page-history
  [{user :identity params :route-params query :query-params}]
  (let [query (validate! query retrieve-page-history-query-schema)
        params (validate! params retrieve-page-history-params-schema)
        params (-> (merge query params)
                   (assoc :type :list/page-history :user user))]
    (->> (sv/query params)
         (p/map #(http/ok (rsp %))))))

;; --- Update Page History

(def update-page-history-schema
  {:label [us/required us/string]
   :pinned [us/required us/boolean]})

(defn update-page-history
  [{user :identity params :route-params data :data}]
  (let [data (validate! data update-page-history-schema)
        params (assoc data
                      :type :update/page-history
                      :id (uuid/from-string (:hid params))
                      :user user)]
    (->> (sv/novelty params)
         (p/map #(http/ok (rsp %))))))
