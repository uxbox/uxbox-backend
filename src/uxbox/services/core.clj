;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.core)

(def +hierarchy+
  (as-> (make-hierarchy) $
    (derive $ :auth/login :command)))

(defmulti -novelty
  (fn [conn data] (:type data))
  :hierarchy #'+hierarchy+)

(defmulti -query
  (fn [conn data] (:type data))
  :hierarchy #'+hierarchy+)

(defmethod -novelty :default
  [data]
  (throw (ex-info "Not implemented" {})))

(defmethod -query :default
  [data]
  (throw (ex-info "Not implemented" {})))

