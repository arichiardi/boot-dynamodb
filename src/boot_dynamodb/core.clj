(ns boot-dynamodb.core
  "Example tasks showing various approaches."
  {:boot/export-tasks true}
  (:require [clojure.string :as str]
            [boot.core :as boot]
            [boot.util :as util]
            [boot.aether :as aether]
            [boot.pod :as pod]))

(def machine-arch (str/lower-case (str (System/getProperty "os.name")
                                       "-"
                                       (System/getProperty "os.arch"))))

(def sqlite4java-version "1.0.392")

(def ^:dynamic
  *pod-env*
  "The pod env, it can be dynamically changed before the first task call
  to run-local if necessary."
  {:name "dynamodb-pod"
   :dependencies (conj '[[arichiardi/boot-dynamodb "0.1.0-SNAPSHOT"]
                         [camel-snake-kebab "0.4.0" :scope "test"]
                         [com.amazonaws/DynamoDBLocal "[1.11,2.0)"]]
                       ['com.almworks.sqlite4java/sqlite4java sqlite4java-version])
   :local-repo (:local-repo (boot/get-env))
   :repositories (conj @aether/default-repositories
                       ["dynamodb-local-oregon" {:url "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"}])})

(defn make-sqlite-native-dep [native-arch native-version]
  [(symbol "com.almworks.sqlite4java"
           (str "libsqlite4java-" native-arch))
   native-version
   :extension (cond
                (re-find #"linux" native-arch) "so"
                (re-find #"win32" native-arch) "dll"
                (re-find #"osx" native-arch) "jnilib")])

(defn- make-pod [sqlite-native-dep]
  (let [new-env (-> (boot/get-env)
                    (assoc :dependencies (:dependencies *pod-env*))
                    (assoc :repositories (:repositories *pod-env*))
                    (update :dependencies conj sqlite-native-dep))]
    (util/dbug* "Pod new env %s\n" (util/pp-str new-env))
    (future (pod/make-pod *pod-env*))))

(boot/deftask local-dynamodb
  "Runs a local instance of DynamoDB

  DynamoDB uses port 8000 by default. If port 8000 is unavailable, this command
  will throw an exception.

  About --delay-transient-statuses: DynamoDB can perform some tasks almost
  instantaneously, such as create/update/delete operations on tables and
  indexes; however, the actual DynamoDB service requires more time for these
  tasks. Setting this parameter helps DynamoDB simulate the behavior of the
  Amazon DynamoDB web service more closely. Currently, this parameter
  introduces delays only for global secondary indexes that are in either
  CREATING or DELETING status.

  If you specify --shared-db, all DynamoDB clients will interact with the same set
  of tables regardless of their region and credential configuration.

  If --in-memory is enabled, when you stop DynamoDB none of the data will be
  saved.

  Note that you cannot specify both --db-path and --in-memory at once.

  For a complete list of DynamoDB runtime options, including --port, type:

    boot run-local -H (note the big H)"
  [c cors    ALLOW #{str} "Enable CORS support for JavaScript. This is an \"allow\" list of specific domains. The default setting is an asterisk (*), which allows public access."
   d db-path PATH  str    "The directory where DynamoDB will write its database file. If you do not specify this option, the file will be written to the current directory."
   t delay-transient-statuses bool "Causes DynamoDB to introduce delays for certain operations."
   H dynamodb-help bool "Prints the local DynamoDB usage summary and options."
   m in-memory     bool "DynamoDB will run in memory, instead of using a database file."
   o optimize-db-before-startup bool "Optimizes the underlying database tables before starting up DynamoDB on your computer. You must also specify -dbPath when you use this parameter."
   p port       PORT int  "The port number that DynamoDB will use to communicate with your application. The default port is 8000."
   s shared-db       bool "DynamoDB will use a single database file, instead of using separate files for each credential and region."
   l log4j-path    PATH str  "The path to the log4j.properties file to use."
   f force-sqlite STR str "Force sqlite4java native library (e.g.: linux-amd64, osx, osx-10.4, ...). Usually autodetected."]
  (let [sqlite-native-dep (make-sqlite-native-dep (or force-sqlite machine-arch)
                                                  sqlite4java-version)
        pod (make-pod sqlite-native-dep)
        opts (into {} (remove (comp nil? val)
                              {:cors cors
                               :db-path db-path
                               :delay-transient-statuses delay-transient-statuses
                               :in-memory in-memory
                               :optimize-db-before-startup optimize-db-before-startup
                               :port port
                               :shared-db shared-db
                               :log4j-path log4j-path
                               :dynamodb-help dynamodb-help
                               :sqlite-native-dep sqlite-native-dep}))]
    (boot/cleanup (pod/with-call-in @pod (boot-dynamodb.pod/stop!)))
    (boot/with-pass-thru fs
      (pod/with-call-in @pod (boot-dynamodb.pod/start! ~opts)))))
