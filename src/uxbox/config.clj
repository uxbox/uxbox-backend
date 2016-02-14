(ns uxbox.config
  "A configuration management."
  (:require [mount.core :as mount :refer (defstate)]
            [environ.core :refer (env)]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(def ^:const +default-config+
  "config/default.edn")

(defn read-config
  []
  (let [defaults (edn/read-string (slurp (io/resource +default-config+)))
        local (io/resource (:local-config env "config/local.edn"))]
    (if local
      (deep-merge defaults (edn/read-string (slurp local)))
      defaults)))

(defstate config
  :start (read-config))
