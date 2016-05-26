(defproject uxbox-backend "0.1.0-SNAPSHOT"
  :description "UXBox backend."
  :url "http://uxbox.github.io"
  :license {:name "MPL 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src" "vendor"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"
             ;; "-Dcom.sun.management.jmxremote.port=9090"
             ;; "-Dcom.sun.management.jmxremote.authenticate=false"
             ;; "-Dcom.sun.management.jmxremote.ssl=false"
             ;; "-Dcom.sun.management.jmxremote.rmi.port=9090"
             ;; "-Djava.rmi.server.hostname=0.0.0.0"
             "-XX:+UseG1GC" "-Xms1g" "-Xmx1g"]
  :dependencies [[org.clojure/clojure "1.9.0-alpha3" :scope "provided"]
                 [org.clojure/tools.logging "0.3.1"]
                 [funcool/struct "0.1.0"]
                 [funcool/suricatta "0.9.0"]
                 [funcool/promesa "1.2.0"]
                 [funcool/catacumba "0.16.0"]

                 [hiccup "1.0.5"]
                 [org.im4java/im4java "1.4.0"]

                 [org.slf4j/slf4j-simple "1.7.21"]
                 [com.layerware/hugsql-core "0.4.7"
                  :exclusions [org.clojure/tools.reader]]
                 [niwinz/migrante "0.1.0"]

                 [buddy/buddy-sign "1.0.0" :exclusions [org.clojure/tools.reader]]
                 [buddy/buddy-hashers "0.14.0"]

                 [org.xerial.snappy/snappy-java "1.1.2.4"]
                 [com.github.spullara.mustache.java/compiler "0.9.1"]
                 [org.postgresql/postgresql "9.4.1208" :scope "provided"]
                 [org.quartz-scheduler/quartz "2.2.3"]
                 [org.quartz-scheduler/quartz-jobs "2.2.3"]
                 [commons-io/commons-io "2.5"]
                 [com.draines/postal "2.0.0"]

                 [hikari-cp "1.7.1"]
                 [mount "0.1.10"]
                 [environ "1.0.3"]])
