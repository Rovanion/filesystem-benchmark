(defproject disk-benchmark "0.1.0-SNAPSHOT"
  :description "A tiny program for benchmarking disk io"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure            "1.10.0"]
                 [org.clojure/tools.cli          "0.4.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [criterium "0.4.4"]]
  :main ^:skip-aot disk-benchmark.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
