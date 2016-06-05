;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.paths
  "Paths related utils."
  (:refer-clojure :exclude [name])
  (:require [storages.core :as st])
  (:import java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.SimpleFileVisitor
           java.nio.file.FileVisitResult
           java.nio.file.LinkOption
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions
           java.nio.file.NoSuchFileException
           ratpack.form.UploadedFile))

(def path
  "Alias to storages.core/path."
  st/path)

(defn parent
  "Get parent path if it exists."
  [path]
  (.getParent ^Path (st/path path)))

(defn base-name
  "Get the file name."
  [path]
  (if (instance? UploadedFile path)
    (.getFileName ^UploadedFile path)
    (str (.getFileName ^Path (st/path path)))))

(defn split-ext
  "Returns a vector of `[name extension]`."
  [path]
  (let [base (base-name path)
        i (.lastIndexOf base ".")]
    (if (pos? i)
      [(subs base 0 i) (subs base i)]
      [base nil])))

(defn extension
  "Return the extension part of a file."
  [path]
  (last (split-ext path)))

(defn name
  "Return the name part of a file."
  [path]
  (first (split-ext path)))
