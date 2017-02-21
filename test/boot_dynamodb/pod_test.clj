(ns boot-dynamodb.pod-test
  (:require [clojure.string :as str]
            [clojure.test :as t :refer [is deftest testing]]
            [boot-dynamodb.pod :as pod]))

(deftest dynamodb-opt-str
  (is (nil? (pod/dynamodb-opt-strs nil)))
  (is (nil? (pod/dynamodb-opt-strs {})))
  (is (= ["-testBool"] (pod/dynamodb-opt-strs (first {:test-bool true}))))
  (is (= ["-testKeyword" "this-is-a-key"] (pod/dynamodb-opt-strs (first {:test-keyword :this-is-a-key}))))
  (is (= ["-testValue" "1"] (pod/dynamodb-opt-strs (first {:test-value 1})))))

(deftest dynamodb-opt-str
  (is (= "-Djava.library.path=./DynamoDBLocal_lib" (pod/java-prop-str (first {"java.library.path" "./DynamoDBLocal_lib"}))))
  (is (thrown? java.lang.AssertionError (pod/java-prop-str (first {:k "./DynamoDBLocal_lib"}))))
  (is (thrown? java.lang.AssertionError (pod/java-prop-str (first {"java.library.path" 1})))))

(deftest dynamodb-cmd-line-options
  (testing "dynamodb option presence"
    (let [s (->> (pod/dynamodb-cmd-line-opts {:cors "http://foo.example"
                                              :db-path "/usr/data/db"
                                              :delay-transient-statuses true
                                              :in-memory true
                                              :optimize-db-before-startup true
                                              :shared-db true
                                              :port 8580
                                              :dynamodb-help true
                                              :whatever "Missing"})
                 (str/join " "))]
      (is (str/includes? s "-cors http://foo.example"))
      (is (str/includes? s "-dbPath /usr/data/db"))
      (is (str/includes? s "-delayTransientStatuses"))
      (is (str/includes? s "-inMemory"))
      (is (str/includes? s "-optimizeDbBeforeStartup"))
      (is (str/includes? s "-sharedDb"))
      (is (str/includes? s "-port 8580"))
      (is (str/includes? s "-help"))
      (is (not (str/includes? s "Missing")))))

  (testing "java option presence"
    (let [opts (pod/java-cmd-line-opts {"java.library.path" "./DynamoDBLocal_lib"
                                        "log4j.configuration" "conf/log4j.properties"
                                        :key-option "Missing"})]
      (is (filter #{"-Djava.library.path=./DynamoDBLocal_lib"} opts))
      (is (filter #{"-Dlog4j.configuration=conf/log4j.properties"} opts))
      (is (not-any? #(str/includes? % "Missing") opts)))))
