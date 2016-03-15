(defproject uxbox-backend "0.1.0-SNAPSHOT"
  :description "UXBox backend."
  :url "http://uxbox.github.io"
  :license {:name "MPL 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.slf4j/slf4j-simple "1.7.18"]
                 [bouncer "1.0.0"]
                 [mount "0.1.10"]
                 [environ "1.0.2"]
                 [buddy/buddy-sign "0.9.0"]
                 [buddy/buddy-hashers "0.11.0"]
                 [com.github.spullara.mustache.java/compiler "0.9.1"]
                 [org.postgresql/postgresql "9.4.1208" :scope "provided"]
                 [niwinz/migrante "0.1.0"]
                 [funcool/suricatta "0.8.1"]
                 [funcool/promesa "0.8.1"]
                 [hikari-cp "1.6.1"]
                 [funcool/catacumba "0.11.3-SNAPSHOT"]])
