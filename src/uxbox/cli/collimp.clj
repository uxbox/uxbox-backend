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
            [uxbox.config]
            [uxbox.migrations]
            [uxbox.util.uuid :as uuid]
            [uxbox.db :as db]
            [uxbox.cli.sql :as sql]
            [uxbox.services.colors :as colors]
            [cuerdas.core :as str]
            [suricatta.core :as sc]
            [storages.core :as st]
            [storages.util :as fs])
  (:import [java.io Reader PushbackReader]))

;; --- Constants

(def ^:const +colors-id-ns+ #uuid "3642a582-565f-4070-beba-af797ab27a6e")

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

(defn- exists-color-collection?
  [conn id]
  {:pre [(uuid? id)]}
  (let [sqlv (sql/get-color-collection {:id id})
        result (sc/fetch-one conn sqlv)]
    (not (empty? result))))

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
