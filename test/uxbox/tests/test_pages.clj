(ns uxbox.tests.test-pages
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [catacumba.testing :refer (with-server)]
            [buddy.core.codecs :as codecs]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.projects :as uspr]
            [uxbox.services.pages :as uspg]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(t/deftest test-http-page-create
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/pages")
              params {:body {:project (:id proj)
                             :name "page1"
                             :data [:test]
                             :width 200
                             :height 200
                             :layout "mobile"}}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:data (:body params)) (:data data)))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "page1")))))))

(t/deftest test-http-page-update
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (th/data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ (str "/api/pages/" (:id page)))
              params {:body (assoc page :data [:test1 :test2])}
              [status page'] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status page')
          (t/is (= 200 status))
          (t/is (= [:test1 :test2] (:data page')))
          (t/is (= 1 (:version page')))
          (t/is (= (:user page') (:id user)))
          (t/is (= (:name data) "page1")))))))

(t/deftest test-http-page-update-metadata
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (th/data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ (str "/api/pages/" (:id page) "/metadata"))
              params {:body (assoc page :data [:test1 :test2])}
              [status page'] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status page')
          (t/is (= 200 status))
          (t/is (= [:test1] (:data page')))
          (t/is (= 1 (:version page')))
          (t/is (= (:user page') (:id user)))
          (t/is (= (:name data) "page1")))))))


(t/deftest test-http-page-delete
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (th/data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ (str "/api/pages/" (:id page)))
              [status response] (th/http-delete user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 204 status))
          (let [sqlv ["SELECT * FROM pages WHERE \"user\"=?" (:id user)]
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))

(t/deftest test-http-page-list
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (th/data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ (str "/api/pages"))
              [status response] (th/http-get user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 200 status))
          (t/is (= 1 (count response))))))))

(t/deftest test-http-page-list-by-project
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj1 (uspr/create-project conn {:user (:id user) :name "proj1"})
          proj2 (uspr/create-project conn {:user (:id user) :name "proj2"})
          data {:user (:id user)
                :version 0
                :data (th/data-encode [])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page1 (uspg/create-page conn (assoc data :project (:id proj1)))
          page2 (uspg/create-page conn (assoc data :project (:id proj2)))]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ (str "/api/projects/" (:id proj1) "/pages"))
              [status response] (th/http-get user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 200 status))
          (t/is (= 1 (count response)))
          (t/is (= (:id (first response)) (:id page1))))))))

