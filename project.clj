(defproject uxbox-backend "0.1.0-SNAPSHOT"
  :description "UXBox backend."
  :url "http://uxbox.github.io"
  :license {:name "MPL 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"
             ;; "-Dcom.sun.management.jmxremote.port=9090"
             ;; "-Dcom.sun.management.jmxremote.authenticate=false"
             ;; "-Dcom.sun.management.jmxremote.ssl=false"
             ;; "-Dcom.sun.management.jmxremote.rmi.port=9090"
             ;; "-Djava.rmi.server.hostname=0.0.0.0"
             "-XX:+UseG1GC" "-Xms200m" "-Xmx200m"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.slf4j/slf4j-simple "1.7.21"]
                 [funcool/struct "0.1.0"]
                 [mount "0.1.10"]
                 [environ "1.0.2"]
                 [buddy/buddy-sign "0.13.0" :exclusions [org.clojure/tools.reader]]
                 [buddy/buddy-hashers "0.14.0"]
                 [org.xerial.snappy/snappy-java "1.1.2.4"]
                 [com.github.spullara.mustache.java/compiler "0.9.1"]
                 [org.postgresql/postgresql "9.4.1208" :scope "provided"]
                 [niwinz/migrante "0.1.0"]
                 [commons-io/commons-io "2.5"]
                 [funcool/suricatta "0.9.0"]
                 [funcool/promesa "1.1.1"]
                 [hikari-cp "1.6.1"]
                 [funcool/catacumba "0.14.0"]])
