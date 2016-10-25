;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns storages.util
  "FileSystem related utils."
  (:require [storages.proto :as pt])
  (:import java.nio.file.Path
           java.nio.file.Files
           java.nio.file.LinkOption
           java.nio.file.OpenOption
           java.nio.file.StandardOpenOption
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions))

(def write-open-opts
  (->> [#_StandardOpenOption/CREATE_NEW
        StandardOpenOption/CREATE
        StandardOpenOption/WRITE]
       (into-array OpenOption)))

(def read-open-opts
  (->> [StandardOpenOption/READ]
       (into-array OpenOption)))

(def follow-link-opts
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn path
  "Create path from string or more than one string."
  ([fst]
   (pt/-path fst))
  ([fst & more]
   (pt/-path (cons fst more))))

(defn make-file-attrs
  "Generate a array of `FileAttribute` instances
  generated from `rwxr-xr-x` kind of expressions."
  [^String expr]
  (let [perms (PosixFilePermissions/fromString expr)
        attr (PosixFilePermissions/asFileAttribute perms)]
    (into-array FileAttribute [attr])))

(defn absolute?
  "Return `true` if the provided path is absolute, `else` in case contrary.
  The `path` parameter can be anything convertible to path instance."
  [path]
  (let [^Path path (pt/-path path)]
    (.isAbsolute path)))

(defn exists?
  "Return `true` if the provided path exists, `else` in case contrary.
  The `path` parameter can be anything convertible to path instance."
  [path]
  (let [^Path path (pt/-path path)]
    (Files/exists path follow-link-opts)))

(defn directory?
  "Return `true` if the provided path is a directory, `else` in case contrary.
  The `path` parameter can be anything convertible to path instance."
  [path]
  (let [^Path path (pt/-path path)]
    (Files/isDirectory path follow-link-opts)))

(defn make-dir!
  "Create a new directory."
  [path]
  (let [^Path path (pt/-path path)
        attrs (make-file-attrs "rwxr-xr-x")]
    (Files/createDirectories path attrs)))


