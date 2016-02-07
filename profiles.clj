{:uberjar {:aot [uxbox.frontend]}
 :playground-client
 {:source-paths ["playground"]
  :main ^:skip-aot msgbus.playground.client}
 :playground-server
 {:source-paths ["playground"]
  :main ^:skip-aot msgbus.playground.server}
 }
