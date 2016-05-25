;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.emails.users
  (:require [uxbox.media :as md]
            [uxbox.emails.core :refer (defemail)]
            [uxbox.emails.layouts :as layouts]))

(defn- register-body-html
  [{:keys [name] :as ctx}]
  [:div
   [:img.img-header {:src (md/resolve-asset "images/img-header.jpg")
                     :alt "UXBOX"}]
   [:div.content
    [:table
     [:tbody
      [:tr
       [:td
        [:h1 "Hi " name]
        [:p "Welcome to uxbox."]
        #_[:p
           [:a.btn-primary {:href "#"} "Sign in"]]
        [:p "Sincerely," [:br] [:strong "The UXBOX Team"]]
        #_[:p "P.S. Having trouble signing up? please contact "
         [:a {:href "#"} "Support email"]]]]]]]])

(defn- register-body-text
  [{:keys [name] :as ctx}]
  (str "Hi " name "\n\n"
       "Welcome to uxbox!\n\n"
       "Sincerely, the UXBOX team.\n"))

(defemail :uxbox/register
  :layout layouts/default
  :subject "UXBOX: Welcome!"
  :body {:text/html register-body-html
         :text/plain register-body-text})
