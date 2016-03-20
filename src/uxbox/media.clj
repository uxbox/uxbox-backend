;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media
  "A media storage persistence layer."
  (:import java.io.File
           java.io.ByteArrayInputStream
           java.io.InputStream
           java.io.OutputStream
           java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.Files
           java.nio.file.LinkOption
           java.nio.file.CopyOptions
           java.nio.file.StandardCopyOptions))

(defprotocol IContent
  (-input-stream [_]))

(defprotocol IStorage
  "A basic abstraction for storage access."
  (-save [_ path content] "Persist the content under specified path.")
  (-delete [_ path] "Delete the file by its path.")
  (-exists? [_ path] "Check if file exists by path."))

(defprotocol IPublicStorage
  (-uri [_ path] "Get a public accessible uri for path."))

(defprotocol IStorageIntrospection
  (-accessed-time [_ path] "Return the last accessed time of the file.")
  (-created-time [_ path] "Return the creation time of the file.")
  (-modified-time [_ path] "Return the last modified time of the file."))

(defprotocol IFSStorage
  "A local filelsystem storage abstraction."
  (-path [_ path] "Return the absolute path to the file."))

;; TODO impl

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Impl
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IContent
  ratpack.http.TypedData
  (-input-stream [this]
    (.getInputStream this)))

(defn path
  "Create path from string or more than one string."
  [fst & more]
  (let [more (into-array String more)]
    (Paths/get ^String fst more)))

(defn- normalize-path
  [^Path base ^Path path]
  (let [^Path path (.normalize path)]
    (when (.isAbsolute path)
      (throw (ex-info "Suspicios operation: absolute path not allowed."
                      {:path (str path)})))
    (let [^Path fullpath (.resolve base path)]
      (when-not (.startsWith fullpath base)
        (throw (ex-info "Suspicios operation: go to parent dir is not allowed."
                        {:path (str path)})))
      fullpath)))

(defn splitext
  [path]
  (if-let [[ name ext] (re-find #"^(.+)(\.\w+)$" (str path))]
    [name (or ext "")]
    [path ""]))

(defrecord FileSystemStorage [^Path base]
  IStorage
  (-save [_ path content]
    (let [^Path path (->> (path pathname)
                          (normalize-path base))
          ^File file (.toFile path)]
      (if (.createNewFile file)
        (let [opts (->> [StandardCopyOption/REPLACE_EXISTING]
                        (into-array CopyOption))]
          (-> (-input-stream content)
              (Files/copy path opts)))
        (throw (ex-info "File already exists." {:path (str path)})))))

  (-delete [_ path]
    (let [path (->> (path pathname)
                    (normalize-path base))]
      (Files/deleteIfExists ^Path path)))

  (-exists? [this pathname]
    (let [path (->> (path pathname)
                    (normalize-path base))
          options (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])]
      (Files/exists path options))))
