(defproject code-viz "0.1.0"
  :description "Visualization of a src directory."
  :url "http://example.com/FIXME"
  :license {:name "MIT"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [rhizome "0.2.7"]
                 [org.clojure/tools.cli "0.4.2"]]

  :plugins [[judepayne/lein-native-image "0.3.1-SNAPSHOT"]]
  
  :main code-viz.core
  :aot [code-viz.core]

  :native-image {:name "code-viz"                 ;; name of output image, optional
;                 :graal-bin "/path/to/graalvm/" ;; path to GraalVM home, optional
                 :opts ["--report-unsupported-elements-at-runtime"
                        "--initialize-at-build-time"
                        "--verbose"]} 

  :profiles {:uberjar
             {:aot :all
              :omit-source false
              :native-image {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}}

  )
