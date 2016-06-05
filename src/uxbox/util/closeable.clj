;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.closeable
  "A closeable abstraction. A drop in replacement for
  clojure builtin `with-open` syntax abstraction."
  (:refer-clojure :exclude [with-open])
  (:require [uxbox.util.files :as files]))

;; --- Public Api

(defprotocol ICloseable
  (-close [_] "Close the resource."))

(defprotocol IDeletable
  (-delete [_] "Remove the resource."))

(defmacro with-open
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))
         (pos? (count bindings))]}
  (reduce (fn [acc bindings]
            `(let ~(vec bindings)
               (try
                 ~acc
                 (finally
                   (-close ~(first bindings))))))
          `(do ~@body)
          (reverse (partition 2 bindings))))

(defmacro with-delete
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))
         (pos? (count bindings))]}
  (reduce (fn [acc bindings]
            `(let ~(vec bindings)
               (try
                 ~acc
                 (finally
                   (-delete ~(first bindings))))))
          `(do ~@body)
          (reverse (partition 2 bindings))))

(defmacro with-clean
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))
         (pos? (count bindings))]}
  (reduce (fn [acc bindings]
            `(let ~(vec bindings)
               (try
                 ~acc
                 (finally
                   (let [v# ~(first bindings)]
                     (cond
                       (satisfies? ICloseable v#) (-close v#)
                       (satisfies? IDeletable v#) (-delete v#)
                       :else (throw (ex-info "Invalid" {}))))))))
          `(do ~@body)
          (reverse (partition 2 bindings))))

;; --- Implementation

(extend-protocol IDeletable
  java.nio.file.Path
  (-delete [this]
    (files/delete this {:recursive true})))

(extend-protocol ICloseable
  java.lang.AutoCloseable
  (-close [this] (.close this)))
