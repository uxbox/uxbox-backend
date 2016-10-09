;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.colors
  (:require [clojure.spec :as s]
            [promesa.core :as p]
            [catacumba.http :as http]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]))

;; --- List Collections

(defn list-collections
  [{user :identity}]
  (let [message {:user user
                 :type :list-color-collections}]
    (->> (sv/query message)
         (p/map #(http/ok (rsp %))))))

;; --- Create Collection

(s/def ::data string?)
(s/def ::create-collection
  (s/keys :req-un [::us/name ::data]
          :opt-un [::us/id]))

(defn create-collection
  [{user :identity data :data}]
  (let [params (us/conform ::create-collection data)
        message (assoc params
                       :type :create-color-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/library/colors/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Collection

(s/def ::update-collection
  (s/merge ::create-collection (s/keys :req-un [::us/version])))

(defn update-collection
  [{user :identity params :route-params data :data}]
  (let [data (us/conform ::update-collection data)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update-color-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))

;; --- Delete Color Collection

(defn delete-collection
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete-color-collection
                 :user user}]
    (->> (sv/novelty message)
         (p/map (fn [v] (http/no-content))))))
