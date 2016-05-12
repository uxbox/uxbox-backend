;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.images
  "Image postprocessing."
  (:require [storages.core :as st]
            [uxbox.media :as media]
            [uxbox.util.paths :as paths]
            [uxbox.util.images :as images]
            [uxbox.util.data :refer (dissoc-in)]))

(defn make-thumbnail
  [path {:keys [size format quality] :as cfg}]
  (let [parent (paths/parent path)
        [filename ext] (paths/split-ext path)

        suffix-parts [(nth size 0) (nth size 1) quality format]
        final-name (apply str filename "-" (interpose "." suffix-parts))
        final-path (paths/path parent final-name)

        images-storage media/images-storage
        thumbs-storage media/thumbnails-storage]
    (if @(st/exists? thumbs-storage final-path)
      (str (st/public-url thumbs-storage final-path))
      (let [datapath @(st/lookup images-storage path)
            content (images/thumbnail datapath cfg)
            path @(st/save thumbs-storage final-path content)]
        (str (st/public-url thumbs-storage path))))))

(defn populate-thumbnail
  [entry {:keys [src dst] :as cfg}]
  (let [src (if (vector? src) src [src])
        dst (if (vector? dst) dst [dst])
        src (get-in entry src)]
     (if (empty? src)
       entry
       (assoc-in entry dst (make-thumbnail src cfg)))))

(defn populate-thumbnails
  [entry & settings]
  (reduce populate-thumbnail entry settings))

(defn populate-urls
  [entry storage src dst]
  (let [src (if (vector? src) src [src])
        dst (if (vector? dst) dst [dst])
        value (get-in entry src)]
    (if (empty? value)
      entry
      (let [url (str (st/public-url storage value))]
        (-> entry
            (dissoc-in src)
            (assoc-in dst url))))))
