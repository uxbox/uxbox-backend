;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns storages.impl
  "Implementation details and helpers."
  (:require [storages.proto :as pt]
            [buddy.core.codecs :as codecs]
            [clojure.java.io :as io])
  (:import java.io.File
           java.io.ByteArrayInputStream
           java.io.InputStream
           java.net.URL
           java.net.URI
           java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.Files
           java.nio.file.LinkOption
           java.nio.file.OpenOption
           java.nio.file.StandardOpenOption
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions))

;; --- Constants

(def open-opts
  (->> [StandardOpenOption/CREATE_NEW
        StandardOpenOption/WRITE]
       (into-array OpenOption)))

(def link-opts
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(def file-attrs
  (let [perms (PosixFilePermissions/fromString "rwxr-xr-x")
        attr (PosixFilePermissions/asFileAttribute perms)]
    (into-array FileAttribute [attr])))

;; --- Helpers

(defn absolute?
  [path]
  (let [^Path path (pt/-path path)]
    (.isAbsolute path)))

(defn exists?
  [path]
  (let [^Path path (pt/-path path)]
    (Files/exists path link-opts)))

(defn directory?
  [path]
  (let [^Path path (pt/-path path)]
    (Files/isDirectory path link-opts)))

(defn create-dir
  [path]
  (let [^Path path (pt/-path path)]
    (Files/createDirectories path file-attrs)))

;; --- Impl

(extend-protocol pt/IContent
  String
  (-input-stream [v]
    (ByteArrayInputStream. (codecs/str->bytes v)))

  Path
  (-input-stream [v]
    (io/input-stream v))

  File
  (-input-stream [v]
    (io/input-stream v))

  URI
  (-input-stream [v]
    (io/input-stream v))

  URL
  (-input-stream [v]
    (io/input-stream v))

  InputStream
  (-input-stream [v]
    v)

  ratpack.http.TypedData
  (-input-stream [this]
    (.getInputStream this)))

(extend-protocol pt/IUri
  URI
  (-uri [v] v)

  String
  (-uri [v] (URI. v)))

(def ^:private empty-string-array
  (make-array String 0))

(extend-protocol pt/IPath
  Path
  (-path [v] v)

  URI
  (-path [v] (Paths/get v))

  URL
  (-path [v] (Paths/get (.toURI v)))

  String
  (-path [v] (Paths/get v empty-string-array))

  clojure.lang.Sequential
  (-path [v]
    (reduce #(.resolve %1 %2)
            (pt/-path (first v))
            (map pt/-path (rest v)))))

(defn- path->input-stream
  [^Path path]
  (->> (into-array OpenOption [StandardOpenOption/READ])
       (Files/newInputStream path)))

(defn- path->output-stream
  [^Path path]
  (->> (into-array OpenOption [StandardOpenOption/WRITE
                               StandardOpenOption/CREATE])
       (Files/newOutputStream path)))

(extend-type Path
  io/IOFactory
  (make-reader [path opts]
    (let [^InputStream is (path->input-stream path)]
      (io/make-reader is opts)))
  (make-writer [path opts]
    (let [^OutputStream os (path->output-stream path)]
      (io/make-writer os opts)))
  (make-input-stream [path opts]
    (let [^InputStream is (path->input-stream path)]
      (io/make-input-stream is opts)))
  (make-output-stream [path opts]
    (let [^OutputStream os (path->output-stream path)]
      (io/make-output-stream os opts))))
