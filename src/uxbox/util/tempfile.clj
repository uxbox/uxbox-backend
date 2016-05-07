;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.tempfile
  "A temporal file abstractions."
  (:require [storages.core :as st])
  (:import java.nio.file.Files
           java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.SimpleFileVisitor
           java.nio.file.FileVisitResult
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions))

(def ^:private default-fileattrs
  (let [perms (PosixFilePermissions/fromString "rwxr-xr-x")
        attr (PosixFilePermissions/asFileAttribute perms)]
    (into-array FileAttribute [attr])))

(defrecord CloseablePath [^Path path]
  java.lang.AutoCloseable
  (close [_]
    (Files/deleteIfExists path)))

(defn create
  "Create a temporal file."
  [& {:keys [suffix prefix wrap]}]
  (let [file (Files/createTempFile prefix suffix default-fileattrs)]
    (if wrap
      (CloseablePath. file)
      file)))

(defn delete
  [path]
  (let [path (st/path path)]
    (Files/deleteIfExists path)))

(defn delete-tree
  [path]
  (let [^Path path (st/path path)]
    (Files/walkFileTree path (proxy [SimpleFileVisitor] []
                               (visitFile [file attrs]
                                 (delete file)
                                 (FileVisitResult/CONTINUE))
                               (postVisitDirectory [dir exc]
                                 (delete dir)
                                 (FileVisitResult/CONTINUE))))))
