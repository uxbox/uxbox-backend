(ns uxbox.frontend
  (:require [mount.core :as mount :refer (defstate)]
            [catacumba.core :as ct]
            [uxbox.frontend.routes :as urt]))

(defstate server
  :start (ct/run-server (urt/app) {:port 5050})
  :stop (.stop server))
