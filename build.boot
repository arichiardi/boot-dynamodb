(def +project+ 'arichiardi/boot-dynamodb)
(def +version+ "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.8.0"]
                            [boot/aether "2.7.1"]
                            [adzerk/bootlaces "0.1.13" :scope "test"]
                            [org.clojure/tools.namespace "0.3.0-alpha3" :scope "test"]
                            [metosin/boot-alt-test "0.3.0" :scope "test"]
                            [camel-snake-kebab "0.4.0" :scope "test"]
                            [com.amazonaws/DynamoDBLocal "[1.11,2.0)" :scope "test"]]
          :repositories #(conj % ["dynamodb-local-oregon" {:url "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"}]))

(require '[metosin.boot-alt-test :refer [alt-test]]
         '[adzerk.bootlaces :refer [bootlaces! build-jar push-snapshot push-release]])

(bootlaces! +version+)

(task-options!
 pom {:project     +project+
      :version     +version+
      :description "Facilities for bootstrapping dynamodb instances"
      :url         "https://github.com/arichiardi/boot-dynamodb"
      :scm         {:url "https://github.com/arichiardi/boot-dynamodb"
                    :connection "scm:git:git@github.com:arichiardi/boot-dynamodb.git"
                    :developerConnection "scm:git:ssh@github.com:arichiardi/boot-dynamodb.git"}
      :license     {"Mozilla Public License 2.0"
                    "https://www.mozilla.org/media/MPL/2.0/index.txt"}})

(def snapshot? #(.endsWith +version+ "-SNAPSHOT"))

(deftask deploy []
  (comp
   (build-jar)
   (if (snapshot?)
     (push-snapshot)
     (push-release))))

(ns-unmap *ns* 'test)

(deftask test []
  (alt-test))
