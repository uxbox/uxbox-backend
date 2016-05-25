;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.emails.layouts)

(defn- default-html
  [body context]
  [:html
   [:head
    [:meta {:http-equiv "Content-Type"
            :content "text/html; charset=UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width"}]
    [:title "title"]
    [:style "style"]]
   [:body {:bgcolor "#f6f6f6"
           :cz-shortcut-listen "true"}
    [:table.body-wrap
     [:tbody
      [:tr
       [:td]
       [:td.container {:bgcolor "#FFFFFF"}
        [:div.logo
         [:img {:src "images/logo.png" :alt "UXBOX"}]]
        body]
       [:td]]]]
    [:table.footer-wrap
     [:tbody
      [:tr
       [:td]
       [:td.container
        [:div.content
         [:table
          [:tbody
           [:tr
            [:td
             [:div {:style "text-align: center;"}
              [:a {:href "#" :target "_blank"}
               [:img {:style "display: inline-block; width: 25px; margin-right: 5px;"
                      :src "images/twitter.png"}]]
              [:a {:href "#" :target "_blank"}
               [:img {:style "display: inline-block; width: 25px; margin-right: 5px;"
                      :src "images/facebook.png"}]]
              [:a {:href "#" :target "_blank"}
               [:img {:style "display: inline-block; width: 25px; margin-right: 5px;"
                      :src "images/linkedin.png"}]]]]]
           [:tr
            [:td {:align "center"}
             [:p
              [:span "Sent from UXBOX | "]
              [:a {:href "#" :target "_blank"}
               [:unsubscribe "Email preferences"]]]]]]]]]
       [:td]]]]]])

(defn default-text
  [body context]
  body)

(def default
  "Default layout instance."
  {:text/html default-html
   :text/plain default-text})
