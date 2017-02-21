(ns ^{:doc "Namespace to be executed inside the boot-dynamodb pod"}
    boot-dynamodb.pod
  (:require [clojure.string :as str]
            [boot.util :as util]
            [camel-snake-kebab.core :as csk])
  (:import (com.amazonaws.services.dynamodbv2.local main.ServerRunner
                                                    server.DynamoDBProxyServer)))

(def ^:private dynamo-option-set #{:cors :db-path :delay-transient-statuses
                                   :in-memory :optimize-db-before-startup
                                   :port :shared-db :dynamodb-help})

(defn dynamodb-opt-strs
  "For each key-value in input, return one or more option strings (in a
  vector)."
  [[k v]]
  (cond
    (= :dynamodb-help k) ["-help"]
    (instance? Boolean v) (if v [(str "-" (csk/->camelCaseString k))] []) ;; no boolean? in 1.8
    (keyword? v) [(str "-" (csk/->camelCaseString k)) (name v)]
    :else [(str "-" (csk/->camelCaseString k)) (str v)]))

(def ^:private dynamodb-option-xf
  (comp (filter (comp dynamo-option-set first))
        (mapcat dynamodb-opt-strs)))

(defn dynamodb-cmd-line-opts [opts]
  (into [] dynamodb-option-xf opts))

(defonce ^:private server (atom nil))

(defn start! ^DynamoDBProxyServer [opts]
  (let [args (dynamodb-cmd-line-opts opts)
        help? (some #{"-help"} args)]
    (try
      (util/dbug* "Starting server with args %s\n" (util/pp-str args))
      (reset! server
              (let [server (ServerRunner/createServerFromCommandLineArgs (into-array String args))]
                (when-not help?
                  (doto server (.start)))))
      (catch Exception e
        (if-not help?
          (util/print-ex e)
          (util/dbug* "Swooshed exception: %s\n" (.getMessage e)))))))

(defn stop! []
  (when-let [s ^DynamoDBProxyServer @server]
    (try
      (.stop s)
      (catch Exception e
        (util/print-ex e))
      (finally
        (try
          (.stop server)
          (catch Exception e
            (util/dbug* "Swooshed exception: %s\n" (.getMessage e))))))))
