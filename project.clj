(defproject uxbox-backend "0.1.0-SNAPSHOT"
  :description "UXBox backend + api"
  :url "http://uxbox.github.io"
  :license {:name "" :url ""}
  :source-paths ["src"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.slf4j/slf4j-simple "1.7.14"]
                 [mount "0.1.9"]
                 [niwinz/migrante "0.1.0-SNAPSHOT"]
                 [funcool/wydra "0.1.0-SNAPSHOT"]
                 [funcool/promesa "0.8.0"]
                 [funcool/catacumba "0.11.1"]]
  :main ^:skip-aot uxbox.frontend
  :plugins [[lein-ancient "0.6.7"]])
