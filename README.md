# boot-dynamodb

[](dependency)
```clojure
[arichiardi/boot-dynamodb "0.1.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

A Boot task to launch a local DynamoDB server (pulling Maven dependencies, not action required on the user end).

## Usage

To use this in your project, require the task:

    (require '[boot-dynamodb.core :refer [local-dynamodb]])

At the moment it only includes a single task that launch a local DynamoDB instance, according to [its official documentation](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html#DynamoDBLocal.DownloadingAndRunning).

To launch a in-memory version, simply call the `local-dynamodb` task:

    $ boot local-dynamodb --in-memory wait

Or you can use persistency:

    $ boot local-dynamodb --shared-db --db-path /your/path/to/dynamodb-local-db wait
    
You can use `boot local-dynamodb -h` for the standard task help or `boot local-dynamodb -H` for the DynamoDB help.

## License

Copyright © 2017 Andrea Richiardi

Distributed under the [Mozilla Public License 2.0](https://www.mozilla.org/media/MPL/2.0/index.txt).
