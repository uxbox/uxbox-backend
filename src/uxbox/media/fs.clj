;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media.fs
  "A local filesystem storage implementation."
  (:require [uxbox.media.proto :as p]
            [uxbox.media.impl :as impl])
  (:import java.io.InputStream
           java.io.OutputStream
           java.nio.file.Path
           java.nio.file.Files
           org.apache.commons.io.IOUtils))

(defn- normalize-path
  [^Path base ^Path path]
  (if (impl/absolute? path)
    (throw (ex-info "Suspicios operation: absolute path not allowed."
                    {:path (str path)}))
    (let [^Path fullpath (.resolve base path)
          ^Path fullpath (.normalize fullpath)]
      (when-not (.startsWith fullpath base)
        (throw (ex-info "Suspicios operation: go to parent dir is not allowed."
                        {:path (str path)})))
      fullpath)))

(defrecord FileSystemStorage [^Path base]
  p/IStorage
  (-save [_ path content]
    (let [^Path path (p/-path path)
          ^Path fullpath (normalize-path base path)]
      (when-not (impl/exists? (.getParent fullpath))
        (impl/create-dir (.getParent fullpath)))
      (try
        (with-open [^InputStream source (p/-input-stream content)
                    ^OutputStream dest (Files/newOutputStream fullpath
                                                              impl/+open-opts+)]
          (IOUtils/copy source dest)
          [path nil])
        (catch java.nio.file.FileAlreadyExistsException err
          [nil err]))))

  (-delete [_ path]
    (let [path (->> (p/-path path)
                    (normalize-path base))]
      (Files/deleteIfExists ^Path path)))

  (-exists? [this path]
    (let [path (->> (p/-path path)
                    (normalize-path base))]
      (impl/exists? path)))

  p/ILocalStorage
  (-lookup [_ path']
    (let [^Path path (->> (p/-path path')
                          (normalize-path base))]
      (when (impl/exists? path)
        path))))

(defrecord PrefixedPathStorage [^FileSystemStorage storage
                                ^Path prefix]
  p/IStorage
  (-save [_ path content]
    (let [^Path path (p/-path path)
          ^Path path (.resolve prefix path)]
      (p/-save storage path content)))

  (-delete [_ path]
    (p/-delete storage path))

  (-exists? [this path]
    (p/-exists? storage path))

  p/ILocalStorage
  (-lookup [_ path]
    (p/-lookup storage path)))

(defn filesystem
  "Create an instance of local FileSystem storage providing an
  absolute base path.

  If that path does not exists it will be automatically created,
  if it exists but is not a directory, an exception will be
  raised."
  [base]
  (let [^Path basepath (p/-path base)]
    (when (and (impl/exists? basepath)
               (not (impl/directory? basepath)))
      (throw (ex-info "File already exists." {})))

    (when-not (impl/exists? basepath)
      (impl/create-dir basepath))

    (->FileSystemStorage basepath)))

(defn prefixed
  "Create a composed storage instance that automatically prefixes
  the path when content is saved. For the rest of methods it just
  relies to the underlying storage.

  This is usefull for atomatically add sertain prefix to some
  uploads."
  [storage prefix]
  (let [prefix (p/-path prefix)]
    (->PrefixedPathStorage storage prefix)))
