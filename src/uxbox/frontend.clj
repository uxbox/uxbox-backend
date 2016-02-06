(ns uxbox.frontend
  (:require [mount.core :as mount]
            [uxbox.frontend.server])
  (:gen-class))

(defn -main
  [& args]
  (mount/start))
