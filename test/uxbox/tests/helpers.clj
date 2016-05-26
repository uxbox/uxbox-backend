(ns uxbox.tests.helpers
  (:refer-clojure :exclude [await])
  (:require [clojure.test :as t]
            [clj-http.client :as http]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [catacumba.serializers :as sz]
            [mount.core :as mount]
            [suricatta.core :as sc]
            [uxbox.services.auth :as usa]
            [uxbox.services.users :as usu]
            [uxbox.util.transit :as transit]
            [uxbox.util.paths :as paths]
            [uxbox.migrations :as umg]
            [uxbox.media :as media]
            [uxbox.db :as db]
            [uxbox.config :as ucfg]))

(def +base-url+ "http://localhost:5050")
(def +config+ (ucfg/read-test-config))

(defn database-reset
  [next]
  (-> (mount/only #{#'uxbox.config/config
                    #'uxbox.db/datasource
                    #'uxbox.migrations/migrations
                    #'uxbox.media/static-storage
                    #'uxbox.media/media-storage
                    #'uxbox.media/images-storage
                    #'uxbox.media/thumbnails-storage
                    #'uxbox.services.auth/secret})
      (mount/swap {#'uxbox.config/config +config+})
      (mount/start))
  (with-open [conn (db/connection)]
    (let [sql (str "SELECT table_name FROM information_schema.tables "
                   " WHERE table_schema = 'public' AND table_name != 'migrations';")
          result (->> (sc/fetch conn sql)
                      (map :table_name))]
      (sc/execute conn (str "TRUNCATE "
                            (apply str (interpose ", " result))
                            " CASCADE;"))))
  (try
    (next)
    (finally
      (mount/stop))))

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
                (transit/decode))]
    [status body]))

(defn http-get
  ([user uri] (http-get user uri nil))
  ([user uri {:keys [query] :as opts}]
   (let [headers (when user
                   {"Authorization" (str "Token " (usa/generate-token user))})
         params (merge {:headers headers}
                       (when query
                         {:query-params query}))]
     (try
       (strip-response (http/get uri params))
       (catch clojure.lang.ExceptionInfo e
         (strip-response (ex-data e)))))))

(defn http-post
  ([uri params]
   (http-post nil uri params))
  ([user uri {:keys [body] :as params}]
   (let [body (-> (transit/encode body)
                  (codecs/bytes->str))
         headers (merge
                  {"content-type" "application/transit+json"}
                  (when user
                    {"Authorization" (str "Token " (usa/generate-token user))}))
         params {:headers headers :body body}]
     (try
       (strip-response (http/post uri params))
       (catch clojure.lang.ExceptionInfo e
         (strip-response (ex-data e)))))))

(defn http-multipart
  [user uri params]
  (let [headers (merge
                  (when user
                    {"Authorization" (str "Token " (usa/generate-token user))}))
        params {:headers headers
                :multipart params}]
    (try
      (strip-response (http/post uri params))
      (catch clojure.lang.ExceptionInfo e
        (strip-response (ex-data e))))))

(defn http-put
  ([uri params]
   (http-put nil uri params))
  ([user uri {:keys [body] :as params}]
   (let [body (-> (transit/encode body)
                  (codecs/bytes->str))
         headers (merge
                  {"content-type" "application/transit+json"}
                  (when user
                    {"Authorization" (str "Token " (usa/generate-token user))}))
         params {:headers headers :body body}]
     (try
       (strip-response (http/put uri params))
       (catch clojure.lang.ExceptionInfo e
         (strip-response (ex-data e)))))))

(defn http-delete
  ([uri]
   (http-delete nil uri))
  ([user uri]
   (let [headers (when user
                   {"Authorization" (str "Token " (usa/generate-token user))})
         params {:headers headers}]
     (try
       (strip-response (http/delete uri params))
       (catch clojure.lang.ExceptionInfo e
         (strip-response (ex-data e)))))))

(defn data-encode
  [data]
  (-> (transit/encode data)
      (codecs/bytes->str)))

(defn create-user
  "Helper for create users"
  [conn i]
  (let [data {:username (str "user" i)
              :password  (hashers/encrypt (str "user" i))
              :metadata (str i)
              :fullname (str "User " i)
              :email (str "user" i "@uxbox.io")}]
    (usu/create-user conn data)))
