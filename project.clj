(defproject filesystem-benchmark "0.1.0-SNAPSHOT"
  :description "A tiny program for benchmarking disk io"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure            "1.10.0"]
                 [org.clojure/tools.cli          "0.4.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojars.cantido/clj-mmap   "2.1.0"]]
  :main ^:skip-aot filesystem-benchmark.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
