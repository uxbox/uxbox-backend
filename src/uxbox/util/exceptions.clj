;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.exceptions
  "A helpers for work with exceptions."
  (:refer-clojure :exclude [ex-info]))

(defn ex-info
  ([type payload] (ex-info type payload ""))
  ([type payload message]
   (clojure.core/ex-info message {:type type :payload payload})))
