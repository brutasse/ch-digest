(defproject ch-digest "0.1"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/tools.cli "0.4.2"]
                 [cljsjs/moment "2.24.0-0"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [irresponsible/tentacles "0.6.3"]
                 [clj-time "0.15.1"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [soda-ash "0.83.0"]
                 [markdown-clj "1.10.0"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :main clubhouse.main
  :source-paths ["src/cljs"]
  :resource-paths ["resources"]
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :profiles {:dev {:dependencies [[day8.re-frame/tracing "0.5.1"]]}}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel true
                        :compiler {:main clubhouse.main
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :asset-path "js/out"
                                   :source-map-timestamp true}}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main clubhouse.main
                                   :output-to "resources/public/js/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
