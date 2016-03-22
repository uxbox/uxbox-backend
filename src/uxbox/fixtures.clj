;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.fixtures
  "A initial fixtures."
  (:require [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [catacumba.serializers :as sz]
            [mount.core :as mount]
            [clj-uuid :as uuid]
            [suricatta.core :as sc]
            [uxbox.config :as cfg]
            [uxbox.persistence :as up]
            [uxbox.migrations]
            [uxbox.services.auth :as sauth]
            [uxbox.services.projects :as sproj]
            [uxbox.services.pages :as spag])
  (:import java.util.UUID))

(defn- mk-uuid
  [prefix i]
  (uuid/v5 uuid/+namespace-oid+ (str prefix i)))

(defn- data-encode
  [data]
  (-> (sz/encode data :transit+json)
      (codecs/bytes->str)))

(defn- create-user
  [conn i]
  (println "create user" i)
  (sauth/create-user conn
                     {:username (str "user" i)
                      :id (mk-uuid "user" i)
                      :password (hashers/encrypt "123123")
                      :email (str "user" i ".test@uxbox.io")}))

(defn- create-project
  [conn i ui]
  (println "create project" i "for user" ui)
  (sproj/create-project conn
                        {:id (mk-uuid "project" i)
                         :user (mk-uuid "user" ui)
                         :name (str "project " i)}))

(defn- create-page
  [conn i pi ui]
  (println "create page" i "for user" ui "for project" pi)
  (spag/create-page conn
                    {:id (mk-uuid "page" i)
                     :user (mk-uuid "user" ui)
                     :project (mk-uuid "project" pi)
                     :data (data-encode nil)
                     :name (str "page " i)
                     :width 1024
                     :height 768
                     :layout "tablet"}))

(defn init
  []
  (mount/start)
  (with-open [conn (up/get-conn)]
    (sc/atomic conn
      (doseq [i (range 10)]
        (create-user conn i))

      (doseq [ui (range 10)]
        (doseq [i (range 10)]
          (create-project conn (str ui i) ui)))

      (doseq [pi (range 10)]
        (doseq [ui (range 10)]
          (doseq [i (range 10)]
            (create-page conn (str pi ui i) (str ui i) ui))))))
  (mount/stop))
