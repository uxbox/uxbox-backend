;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.cli.collimp
  "Collection importer command line helper."
  (:require [clojure.pprint :refer (pprint)]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [cuerdas.core :as str]
            [suricatta.core :as sc]
            [storages.core :as st]
            [storages.util :as fs]
            [uxbox.config]
            [uxbox.db :as db]
            [uxbox.cli.sql :as sql]
            [uxbox.migrations]
            [uxbox.media :as media]
            [uxbox.util.uuid :as uuid]
            [uxbox.util.data :as data])
  (:import [java.io Reader PushbackReader]
           [javax.imageio ImageIO]))

;; --- Constants

(def ^:const +imates-uuid-ns+ #uuid "3642a582-565f-4070-beba-af797ab27a6e")

;; --- CLI Helpers

(defn exit
  ([] (exit 0))
  ([code]
   (System/exit code)))

(defn printerr
  [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn pushback-reader
  [reader]
  (PushbackReader. ^Reader reader))

;; --- Colors Collections Importer

(def storage media/images-storage)

(defn- create-image-collection
  "Create or replace image collection by its name."
  [conn {:keys [name] :as entry}]
  (let [id (uuid/namespaced +imates-uuid-ns+ name)
        sqlv (sql/create-image-collection {:id id :name name})]
    (sc/execute conn sqlv)
    id))

(defn- retrieve-image-size
  [path]
  (let [path (fs/path path)
        file (.toFile path)
        buff (ImageIO/read file)]
    [(.getWidth buff)
     (.getHeight buff)]))

(defn- retrieve-image
  [conn id]
  {:pre [(uuid? id)]}
  (let [sqlv (sql/get-image {:id id})]
    (some->> (sc/fetch-one conn sqlv)
             (data/normalize-attrs))))

(defn- delete-image
  [conn {:keys [id path] :as image}]
  {:pre [(uuid? id)
         (fs/path? path)]}
  (let [sqlv (sql/delete-image {:id id})]
    @(st/delete storage path)
    (sc/execute conn sqlv)))

(defn- create-image
  [conn collid imageid localpath]
  {:pre [(fs/path? localpath)
         (uuid? collid)
         (uuid? imageid)]}
  (let [filename (fs/base-name localpath)
        [width height] (retrieve-image-size localpath)
        extension (second (fs/split-ext filename))
        path @(st/save storage filename localpath)
        params {:name filename
                :path (str path)
                :mimetype (case extension
                            ".jpg" "image/jpeg"
                            ".png" "image/png")
                :width width
                :height height
                :collection collid
                :id imageid}
        sqlv (sql/create-image params)]
    (sc/execute conn sqlv)))

(defn- import-image-from-path
  [conn id fpath]
  {:pre [(uuid? id)
         (fs/path? fpath)]}
  (let [imageid (uuid/namespaced +imates-uuid-ns+ (str id fpath))]
    (if-let [image (retrieve-image conn imageid)]
      (do
        (delete-image conn image)
        (create-image conn id imageid fpath))
      (create-image conn id imageid fpath))))

(defn- process-images-entry-by-dir
  [conn {:keys [path id regex] :as entry}]
  {:pre [(fs/path? path)
         (uuid? id)
         (instance? java.util.regex.Pattern regex)]}
  (doseq [fpath (fs/list-files path)]
    (when (re-matches regex (str fpath))
      (import-image-from-path conn id fpath))))

(defn- process-images-entry-by-list
  [conn {:keys [data id] :as entry}]
  {:pre [(uuid? id)
         (set? data)]}
  (doseq [item data]
    (import-image-from-path conn id name (fs/path item))))

(defn- process-images-entry
  [conn {:keys [type] :as entry}]
  {:pre [(keyword? type)]}
  (let [id (create-image-collection conn entry)
        entry (assoc entry :id id)]
    (case type
      :dir (process-images-entry-by-dir conn entry)
      :list (process-images-entry-by-list conn entry))))

;; --- Entry Point

(defn- check-path
  [path]
  (when-not path
    (printerr "No path is provided.")
    (exit -1))
  (when-not (fs/exists? path)
    (printerr "Path does not exists.")
    (exit -1))
  (when (fs/directory? path)
    (printerr "The provided path is a directory.")
    (exit -1))
  (fs/path path))

(defn- read-import-file
  [path]
  (let [path (check-path path)
        reader (pushback-reader (io/reader path))]
    (read reader)))

(defn- start-system
  []
  (-> (mount/only #{#'uxbox.config/config
                    #'uxbox.db/datasource
                    #'uxbox.migrations/migrations})
      (mount/start)))

(defn- stop-system
  []
  (mount/stop))

(defn- run-importer
  [data]
  (println "Running importer on:")
  (pprint data))

(defn main
  [& [path]]
  (let [data (read-import-file path)]
    (start-system)
    (try
      (run-importer data)
      (finally
        (stop-system)))))
