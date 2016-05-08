;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.files
  "Helpers related to files and directories."
  (:refer-clojure :exclude [name])
  (:require [storages.core :as st])
  (:import java.nio.file.Files
           java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.SimpleFileVisitor
           java.nio.file.FileVisitResult
           java.nio.file.LinkOption
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions
           java.nio.file.NoSuchFileException
           org.apache.commons.io.FilenameUtils
           ratpack.form.UploadedFile))

(defn exists?
  "Check if a path exists."
  [path]
  (let [^Path path (st/path path)
        opts (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])]
    (Files/exists path opts)))

(defn- delete-recursive
  [^Path path]
  (try
    (Files/walkFileTree path (proxy [SimpleFileVisitor] []
                               (visitFile [file attrs]
                                 (Files/deleteIfExists file)
                                 (FileVisitResult/CONTINUE))
                               (postVisitDirectory [dir exc]
                                 (Files/deleteIfExists dir)
                                 (FileVisitResult/CONTINUE))))
    true
    (catch NoSuchFileException e
      false)))

(defn delete
  "Delete a file or directory."
  ([path] (delete path nil))
  ([path {:keys [recursive] :or {recursive false}}]
   (let [^Path path (st/path path)]
     (if recursive
       (delete-recursive path)
       (Files/deleteIfExists path)))))



