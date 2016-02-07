;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.executor
  "A basic abstraction for executor services."
  (:import java.util.concurrent.ForkJoinPool
           java.util.concurrent.Executor
           java.util.concurrent.Executors
           java.util.concurrent.ThreadFactory))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The main abstraction definition.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IExecutor
  (-execute [_ task] "Execute a task in a executor."))

;; (defprotocol IExecutorService
;;   (-submit [_ task] "Submit a task and return a promise."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type Executor
  IExecutor
  (-execute [this task]
    (.execute this ^Runnable task)))

(defn- thread-factory-adapter
  "Adapt a simple clojure function into a
  ThreadFactory instance."
  [func]
  (reify ThreadFactory
    (^Thread newThread [_ ^Runnable runnable]
      (func runnable))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +default+ (ForkJoinPool/commonPool))
(def ^:const +default-thread-factory+ (Executors/defaultThreadFactory))

(defn execute!
  "Execute a task in a provided executor.

  A task is a plain clojure function or
  jvm Runnable instance."
  ([task]
   (-execute +default+ task))
  ([executor task]
   (-execute executor task)))
