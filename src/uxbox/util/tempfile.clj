;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.tempfile
  "A temporal file abstractions."
  (:require [storages.core :as st]
            [uxbox.util.paths :as paths])
  (:import java.nio.file.Files
           java.nio.file.Path
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions))

(defrecord CloseablePath [^Path path]
  java.lang.AutoCloseable
  (close [_]
    (Files/deleteIfExists path)))

(defn create
  "Create a temporal file."
  [& {:keys [suffix prefix wrap]}]
  (let [perms (PosixFilePermissions/fromString "rwxr-xr-x")
        attr (PosixFilePermissions/asFileAttribute perms)
        attrs (into-array FileAttribute [attr])
        file (Files/createTempFile prefix suffix attrs)]
    (if wrap
      (CloseablePath. file)
      file)))
