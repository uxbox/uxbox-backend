;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.emails.users
  (:require [uxbox.emails.core :refer (defemail)]
            [uxbox.emails.layouts :as layouts]))

(defn- register-body-html
  [{:keys [name] :as ctx}]
  [:div
   [:img.img-header {:src "images/img-header.jpg" :alt "UXBOX"}]
   [:div.content
    [:table
     [:tbody
      [:tr
       [:td
        [:h1 "Hi" name]
        [:p
         "Lorem ipsum dolor sit amet, " [:strong "UXBOX"]
         "adipiscing elit. Etiam ultricies, erat in cursus euismod, libero "
         "metus maximus augue, id efficitur neque massa et mi. Nunc eros leo,"
         "varius at odio molestie, feugiat auctor diam. Nullam ut ipsum nec "
         "velit laoreet egestas sit amet sollicitudin odio."]
        [:p
         [:a.btn-primary {:href "#"} "Sign in"]]
        [:p "Welcome to the UXBOX community"]
        [:p "Sincerely," [:br] [:strong "The UXBOX Team"]]
        [:p "P.S. Having trouble signing up? please contact "
         [:a {:href "#"} "Support email"]]]]]]]])

(def ^:private register-body-text
  (constantly ""))

(defemail :uxbox/register
  :layout layouts/default
  :subject "UXBOX: Welcome!"
  :body {:text/html register-body-html
         :text/plain register-body-text})

;; (comment
;;   (emails/send! {:email/priority :high
;;                  :email/name :uxbox/register
;;                  :email/to "pepa@gmail.com"
;;                  :username "pepito"
;;                  :email "pepito@gmail.com"}))

