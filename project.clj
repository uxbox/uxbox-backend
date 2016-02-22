(defproject uxbox-backend "0.1.0-SNAPSHOT"
  :description "UXBox backend."
  :url "http://uxbox.github.io"
  :license {:name "" :url ""}
  :source-paths ["src"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.slf4j/slf4j-simple "1.7.16"]
                 [bouncer "1.0.0"]
                 [mount "0.1.9"]
                 [environ "1.0.2"]
                 [buddy/buddy-sign "0.9.0"]
                 [buddy/buddy-hashers "0.11.0"]
                 [com.github.spullara.mustache.java/compiler "0.9.1"]
                 [niwinz/migrante "0.1.0-SNAPSHOT"]
                 [funcool/suricatta "0.8.1"]
                 [funcool/promesa "0.8.1"]
                 [hikari-cp "1.5.0"]
                 [funcool/catacumba "0.11.2-SNAPSHOT"]]
  :profiles
  {:dev {:plugins [[lein-ancient "0.6.7"]]
         :dependencies [[clj-http "2.1.0"]]
         :main ^:skip-aot uxbox.main}})
