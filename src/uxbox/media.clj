(ns uxbox.media
  "A media storage persistence layer.")

(defprotocol IStorage
  "A basic abstraction for storage access."
  (-save [_ path content] "Persist the content under specified path.")
  (-delete [_ path] "Delete the file by its path.")
  (-exists? [_ path] "Check if file exists by path."))

(defprotocol IPublicStorage
  (-uri [_ path] "Get a public accessible uri for path."))

(defprotocol IStorageIntrospection
  (-accessed-time [_ path] "Return the last accessed time of the file.")
  (-created-time [_ path] "Return the creation time of the file.")
  (-modified-time [_ path] "Return the last modified time of the file."))

(defprotocol IFSStorage
  "A local filelsystem storage abstraction."
  (-path [_ path] "Return the absolute path to the file."))

;; TODO impl
