(defproject socket-cl2 "0.2.0"
  :description "Websocket/sockjs wrappers/macros for ChlorineJS"
  :url "https://github.com/chlorinejs/socket-cl2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :node-dependencies [[underscore "1.4.3"]
                      [nyancat "0.0.3"]]
  :nodejs {:keywords ["qunit",
                      "chlorinejs",
                      "clojure",
                      "websocket",
                      "sockjs",
                      "macro"]}
  :plugins [[lein-cl2c "0.0.1-SNAPSHOT"]
            [lein-npm "0.2.0"]])
