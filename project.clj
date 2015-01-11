(defproject elite-mfd "0.1.0-SNAPSHOT"
  :description "Fancy, contextual Elite: Dangerous tools"
  :url "http://github.com/dhleong/elite-mfd"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.nrepl "0.2.5"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojars.dhleong/speech-synthesis "1.0.1"]
                 [http-kit "2.1.16"]
                 [http-kit.fake "0.2.1"]
                 [cheshire "5.4.0"]
                 [compojure "1.1.8"]]
  :main ^:skip-aot elite-mfd.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
