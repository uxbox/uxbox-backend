(ns uxbox.tests.helpers
  (:refer-clojure :exclude [await])
  (:require [clojure.test :as t]
            [clj-http.client :as http]
            [buddy.core.codecs :as codecs]
            [catacumba.serializers :as sz]
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


(defn- strip-response
  [{:keys [status headers body]}]
  (if (= (get headers "content-type") "application/transit+json")
    [status (-> (codecs/str->bytes body)
                (sz/decode :transit+json))]
    [status body]))

(defn post
  [uri body]
  (let [body (-> (sz/encode body :transit+json)
                 (codecs/bytes->str))
        headers {"content-type" "application/transit+json"}
        params {:body body :headers headers}]
    (try
      (strip-response (http/post uri params))
      (catch clojure.lang.ExceptionInfo e
        (strip-response (ex-data e))))))
