(ns boot-dynamodb.pod-test
  (:require [clojure.string :as str]
            [clojure.test :as t :refer [is deftest testing]]
            [boot-dynamodb.pod :as pod]))

(deftest dynamodb-opt-str
  (is (= [] (pod/dynamodb-opt-strs nil)))
  (is (= ["-testBool"] (pod/dynamodb-opt-strs (first {:test-bool true}))))
  (is (= ["-testKeyword" "this-is-a-key"] (pod/dynamodb-opt-strs (first {:test-keyword :this-is-a-key}))))
  (is (= ["-testValue" "1"] (pod/dynamodb-opt-strs (first {:test-value 1})))))

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
      (is (not (str/includes? s "Missing"))))))
