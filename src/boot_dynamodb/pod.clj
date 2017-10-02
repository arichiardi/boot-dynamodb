(ns ^{:doc "Namespace to be executed inside the boot-dynamodb pod"}
    boot-dynamodb.pod
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :as csk]
            [boot.util :as util]
            [boot.aether :as aether]
            [boot.pod :as pod])
  (:import (com.amazonaws.services.dynamodbv2.local main.ServerRunner
                                                    server.DynamoDBProxyServer)))

(def ^:private dynamo-option-set #{:cors :db-path :delay-transient-statuses
                                   :in-memory :optimize-db-before-startup
                                   :port :shared-db :dynamodb-help})

(defn dynamodb-opt-strs
  "For each key-value in input, return one or more option strings (in a
  vector)."
  [[k v :as opts]]
  (cond
    (nil? opts) []
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

(defn- set-sqlite-native! [sqlite-native-dep]
  (let [sqlite-resolved-deps (some-> pod/env
                                     (assoc :dependencies [sqlite-native-dep])
                                     (aether/resolve-dependencies))
        sqlite-native-folder (some->> sqlite-resolved-deps
                                      (filter #(= sqlite-native-dep (:dep %)))
                                      first
                                      :jar
                                      io/file
                                      .getParent)]
    (util/dbug* "Sqlite4java resolved to folder %s\n" sqlite-native-folder)
    (if sqlite-native-folder
      (do (System/setProperty "sqlite4java.library.path" sqlite-native-folder)
          (util/dbug* "Set sqlite4java.library.path to %s\n" (System/getProperty "sqlite4java.library.path")))
      (util/warn "Cannot set java.library.path: the Sqlite4java dependency resolved to nil. DynamoDB might not work correctly."))))

(defn start! ^DynamoDBProxyServer [opts]
  (let [args (dynamodb-cmd-line-opts opts)
        help? (some #{"-help"} args)]
    (try
      (when-let [log4j-path (:log4j-path opts)]
        (util/dbug* "Setting up log4j file %s\n" log4j-path)
        (System/setProperty "log4j.configuration" log4j-path))
      (when-let [sqlite-native-dep (:sqlite-native-dep opts)]
        (util/dbug* "Setting up Sqlite4java Native Library from vector %s\n" sqlite-native-dep)
        (set-sqlite-native! sqlite-native-dep))
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
