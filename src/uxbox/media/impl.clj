;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media.impl
  "Implementation details and helpers."
  (:require [uxbox.media.proto :as p]
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
  (let [^Path path (p/-path path)]
    (.isAbsolute path)))

(defn exists?
  [path]
  (let [^Path path (p/-path path)]
    (Files/exists path link-opts)))

(defn directory?
  [path]
  (let [^Path path (p/-path path)]
    (Files/isDirectory path link-opts)))

(defn create-dir
  [path]
  (let [^Path path (p/-path path)]
    (Files/createDirectories path file-attrs)))

;; --- Impl

(extend-protocol p/IContent
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

(extend-protocol p/IPath
  Path
  (-path [v] v)

  String
  (-path [v] (Paths/get v (make-array String 0)))

  clojure.lang.Sequential
  (-path [v]
    (let [fv (first v)
          mv (rest v)]
      (Paths/get ^String fv (into-array String mv)))))

(extend-type Path
  io/IOFactory
  (make-reader [path opts]
    (let [^File file (.toFile path)]
      (io/make-reader file opts)))
  (make-writer [path opts]
    (let [^File file (.toFile path)]
      (io/make-writer file opts)))
  (make-input-stream [path opts]
    (let [^File file (.toFile path)]
      (io/make-input-stream file opts)))
  (make-output-stream [path opts]
    (let [^File file (.toFile path)]
      (io/make-output-stream file opts))))
