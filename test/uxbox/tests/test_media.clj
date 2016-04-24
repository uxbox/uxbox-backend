(ns uxbox.tests.test-media
  (:require [clojure.test :as t]
            [uxbox.media :as um]
            [uxbox.media.fs :as umfs]
            [uxbox.media.misc :as umsc])
  (:import java.io.File
           org.apache.commons.io.FileUtils))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- clean-temp-directory
  [next]
  (next)
  (let [directory (File. "/tmp/catacumba/")]
    (FileUtils/deleteDirectory directory)))

(t/use-fixtures :each clean-temp-directory)

;; --- Tests: FileSystemStorage

(t/deftest test-localfs-store-and-lookup
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        rpath  @(um/save storage "test.txt" "my content")
        fpath @(um/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/catacumba/test/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-store-and-lookup-with-subdirs
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        rpath  @(um/save storage "somepath/test.txt" "my content")
        fpath @(um/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/catacumba/test/somepath/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-store-and-delete-and-check
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        rpath  @(um/save storage "test.txt" "my content")]
    (t/is @(um/delete storage rpath))
    (t/is (not @(um/exists? storage rpath)))))

(t/deftest test-localfs-store-duplicate-file-raises-exception
  (let [storage (umfs/filesystem "/tmp/catacumba/test")]
    (t/is @(um/save storage "test.txt" "my content"))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/save storage "test.txt" "my content")))))

(t/deftest test-localfs-access-unauthorized-path
  (let [storage (umfs/filesystem "/tmp/catacumba/test")]
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/lookup storage "../test.txt")))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/lookup storage "/test.txt")))))

;; --- Tests: PrefixedPathStorage

(t/deftest test-localfs-prefixed-store-and-lookup
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        storage (umsc/prefixed storage "some/prefix")
        rpath  @(um/save storage "test.txt" "my content")
        fpath @(um/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/catacumba/test/some/prefix/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-prefixed-store-and-delete-and-check
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        storage (umsc/prefixed storage "some/prefix")
        rpath  @(um/save storage "test.txt" "my content")]
    (t/is @(um/delete storage rpath))
    (t/is (not @(um/exists? storage rpath)))))

(t/deftest test-localfs-prefixed-store-duplicate-file-raises-exception
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        storage (umsc/prefixed storage "some/prefix")]
    (t/is @(um/save storage "test.txt" "my content"))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/save storage "test.txt" "my content")))))

(t/deftest test-localfs-prefixed-access-unauthorized-path
  (let [storage (umfs/filesystem "/tmp/catacumba/test")
        storage (umsc/prefixed storage "some/prefix")]
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/lookup storage "../test.txt")))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(um/lookup storage "/test.txt")))))

