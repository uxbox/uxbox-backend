(ns uxbox.tests.helpers
  (:refer-clojure :exclude [await])
  (:require [clojure.test :as t]
            [clj-http.client :as http]
            [buddy.core.codecs :as codecs]
            [catacumba.handlers.postal :as pc]
            [mount.core :as mount]
            [suricatta.core :as sc]
            [uxbox.migrations :as umg]
            [uxbox.persistence :as up]
            [uxbox.config :as ucfg]))

(def +config+ (ucfg/read-test-config))
(def +ds+ (up/create-datasource (:database +config+)))

(defn database-reset
  [next]
  (with-open [conn (sc/context +ds+)]
    (sc/execute conn "drop schema if exists public cascade;")
    (sc/execute conn "create schema public;"))
  (mount/start-with {#'uxbox.config/config +config+})
  (next)
  (mount/stop))

(defn ex-info?
  [v]
  (instance? clojure.lang.ExceptionInfo v))

(defmacro await
  [expr]
  `(try
     (deref ~expr)
     (catch Exception e#
       (.getCause e#))))

(defn send-frame
  [uri frame]
  (let [data (pc/-encode frame :application/transit+json)
        headers {"content-type" "application/transit+json"}
        params {:body data :headers headers}
        response  (http/put uri params)]
    (if (= (:status response) 200)
      (-> (:body response)
          (codecs/str->bytes)
          (pc/-decode :application/transit+json))
      (throw (ex-info "Wrong response" response)))))

