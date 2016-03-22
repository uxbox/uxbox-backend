;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.uuid
  (:import java.util.UUID))

(defn from-string
  "Parse string uuid representation into proper UUID instance."
  [s]
  (UUID/fromString s))

