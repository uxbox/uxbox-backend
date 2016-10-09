;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.images
  (:require [clojure.spec :as s]
            [promesa.core :as p]
            [catacumba.http :as http]
            [storages.core :as st]
            [uxbox.media :as media]
            [uxbox.images :as images]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]
            [uxbox.util.paths :as paths]))

;; --- Constants & Config

(def +thumbnail-options+ {:src :path
                          :dst :thumbnail
                          :size [300 110]
                          :quality 92
                          :format "jpg"})

;; --- Create Collection

(s/def ::create-collection
  (s/keys :req-un [::us/name] :opt-un [::us/id]))

(defn create-collection
  [{user :identity data :data}]
  (let [data (us/conform ::create-collection data)
        message (assoc data
                       :type :create-image-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/library/images/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Collection

(s/def ::update-collection
  (s/merge ::create-collection (s/keys :req-un [::us/version])))

(defn update-collection
  [{user :identity params :route-params data :data}]
  (let [data (us/conform ::update-collection data)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update-image-collection
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Collection

(defn delete-collection
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete-image-collection
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))

;; --- List collections

(defn list-collections
  [{user :identity}]
  (let [params {:user user
                :type :list-image-collections}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

;; --- Create image

(s/def ::file ::us/uploaded-file)
(s/def ::width integer?)
(s/def ::height integer?)
(s/def ::create-image
  (s/keys :req-un [::file ::width ::height] :opt-un [::us/id]))

(defn create-image
  [{user :identity params :route-params  data :data}]
  (let [{:keys [file id width height] :as data} (us/conform ::create-image data)
        id (or id (uuid/random))
        filename (paths/base-name file)
        storage media/images-storage]
    (letfn [(persist-image-entry [path]
              (sv/novelty {:id id
                           :type :create-image
                           :user user
                           :width width
                           :height height
                           :collection (uuid/from-string (:id params))
                           :name filename
                           :path (str path)}))

            (populate-thumbnails [entry]
              (images/populate-thumbnails entry +thumbnail-options+))

            (populate-urls [v]
              (images/populate-urls v storage :path :url))

            (create-response [entry]
              (let [loc (str "/api/library/images/" (:id entry))]
                (http/created loc (rsp entry))))]

      (->> (st/save storage filename file)
           (p/mapcat persist-image-entry)
           (p/map populate-thumbnails)
           (p/map populate-urls)
           (p/map create-response)))))

;; --- Update Image

(s/def ::update-image
  (s/keys :req-un [::us/name ::us/version] :opt-un [::us/id]))

(defn update-image
  [{user :identity params :route-params data :data}]
  (let [data (s/conform ::update-image data)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update-image
                       :user user)]
    (->> (sv/novelty message)
         (p/map #(http/ok (rsp %))))))

;; --- Delete Image

(defn delete-image
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete-image
                 :user user}]
    (->> (sv/novelty message)
         (p/map (fn [v] (http/no-content))))))

;; --- List collections

(defn list-images
  [{user :identity route-params :route-params}]
  (let [params {:collection (uuid/from-string (:id route-params))
                :user user
                :type :list-images}
        thumbnail-opts +thumbnail-options+
        populate-thumbnails-url #(images/populate-thumbnails % thumbnail-opts)]
    (->> (sv/query params)
         (p/map #(http/ok (rsp (map populate-thumbnails-url %)))))))
