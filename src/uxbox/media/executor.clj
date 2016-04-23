;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media.executor
  "A basic abstraction for executor services."
  (:require [promesa.core :as p])
  (:import java.util.concurrent.ForkJoinPool
           java.util.concurrent.Executor
           java.util.concurrent.Executors))

;; --- Impl

(defprotocol IExecutor
  (^:private -execute [_ task] "Execute a task in a executor."))

(defprotocol IExecutorService
  (^:private -submit [_ task] "Submit a task and return a promise."))

(extend-type Executor
  IExecutor
  (-execute [this task]
    (.execute this ^Runnable task))

  IExecutorService
  (-submit [this task]
    (p/promise
     (fn [resolve reject]
       (-execute this #(try
                         (resolve (task))
                         (catch Throwable e
                           (reject e))))))))

;; --- Public Api

(def ^:redef default (ForkJoinPool/commonPool))

(defn execute
  "Execute a task in a provided executor.

  A task is a plain clojure function or
  jvm Runnable instance."
  ([task]
   (-execute default task))
  ([executor task]
   (-execute executor task)))

(defn submit
  "Submit a task to be executed in a provided executor
  and return a promise that will be completed with
  the return value of a task.

  A task is a plain clojure function."
  ([task]
   (-submit default task))
  ([executor task]
   (-submit executor task)))
