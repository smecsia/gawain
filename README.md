# Gawain

Microframework focusing on data processing and aggregation in distributed environment

## Setup
build.gradle
```groovy
    compile 'ru.qatools:gawain:1.0'
```

## Usage

Gawain is an event-driven framework which operates with terms like `queue`, `event`, `state`, `processor`, `aggregator`, `repository`, `broadcaster`, `timer`. 
* `event` is a message that can affect system. It is enqueued into the `queue` and later is processed by `processor` or `aggregator`.
* `queue` is a storage, where messages reside until they are processed by `processor` or `aggregator`.
* Unlike the `queue`, `broadcaster` is a message broker that routes messages to every node of the cluster.
* `processor` - is a stateless lambda, which can modify event and then route it to the next `processor` or `aggregator`.
* `aggregator` - is a stateful `processor`, which holds the state consistently as a [distributed map](http://en.wikipedia.org/wiki/Distributed_hash_table) 
of [correlation identifiers](http://camel.apache.org/correlation-identifier.html) and values. Aggregation can happen 
on any node of the cluster and thus must be performed concurrently. Gawain uses the 
[distributed locks](http://en.wikipedia.org/wiki/Distributed_lock_manager) for that purpose.
* `timer` is a scheduled job, which can perform any operations periodically with given schedule.

```groovy
def gawain = Gawain.run {
    processor('male', filter { it != 'Ivanov' }, process { "Mr. ${it}" }).to('people')
    
    processor('female', process { "Mrs. ${it}" }).to('people')
    
    aggregator 'people', key { it }, 
            aggregate { state, evt -> state.name = evt }
}

// feeding up the queues with events
['Ivanov', 'Petrov', 'Sidorov'].each { gawain.to('male', it) }
['Ivanova', 'Petrova'].each { gawain.to('female', it) }
// when processing is completed
// this will print "Mr. Petrov, Mr. Sidorov, Mrs. Ivanova, Mrs. Petrova"
println(gawain.repo('people').keys())
```

### Routing


### Scheduled processors

You can register the scheduled jobs, that can access or modify data, or just perform any other operations within given
schedule:

```groovy
doEvery(60, SECONDS, { println("Another minute of your life has just been wasted!" })
```

The following example shows how to access and modify data from aggregator:

```groovy
def gawain = Gawain.run {
    aggregator 'input', key { it.id },
            aggregate { state, evt ->
                state.ticks = 0
                state.evt = evt
            }
    doEvery(300, MILLISECONDS, {
        repo('input').withEach { key, state ->
            state.ticks += 1
        }
    })
}

// this will create state with key 'Vasya':
gawain.to('input', [id: 'Vasya'])

// after 900ms this will print 3
println(gawain.repo('input')['Vasya'].ticks)
```

By default scheduled processors perform on every cluster node. To make them global (to be perfomed on "master" 
node only) you should pass 'global' option in definition:

```groovy
doEvery(100, MILLISECONDS, { println("Hello from master node!" }, global: true)
```

### ActiveMQ as a message broker and broadcaster

build.gradle
```groovy
compile 'ru.qatools:gawain-activemq:1.0'
```

Somewhere in your code:
```groovy
// ... 
def factory = new ActiveMQConnectionFactory()
factory.brokerURL = 'tcp://localhost:61616'
Gawain.run {
     useQueueBuilder(
        new ActivemqQueueBuilder(
                factory.createConnection() as ActiveMQConnection
        )         
     )
     useBroadcastBuilder(
        new ActivemqBroadcastBuilder(
                factory.createConnection() as ActiveMQConnection
        )         
     )
    // Now all messages will be handled via ActiveMQ broker and its queues
    // All nodes connected to the same ActiveMQ will be listening all broadcasting events
}
```

### MongoDB as a repository and broadcaster (and also as a distributed locks engine)

build.gradle
```groovy
compile 'ru.qatools:gawain-mongodb:1.0'
```

Somewhere in your code:
```groovy
// ... 
def dbURL = 'mongodb://user:password@localhost:27017/database?w=majority'
def dbName = 'database'
Gawain.run {
     useRepoBuilder(
        new MongodbRepoBuilder(dbURL, dbName)
     )
     useBroadcastBuilder(
        new MongodbBroadcastBuilder(dbURL, dbName)
     )
    // Now all the data of aggregators will be stored within MongoDB
    // All nodes connected to the same MongoDB will be listening all broadcasting events
}
```

### JDBC database (MySQL, PostgreSQL, H2) as a repository (and distributed locks engine)

build.gradle
```groovy
compile 'ru.qatools:gawain-jdbc:1.0'
```

For H2 database:
```groovy
// ... 
Gawain.run {
     useRepoBuilder(
        new JDBCRepoBuilder(DriverManager.getConnection('jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1'))
     )
    // ...
}
```

For MySQL database:
```groovy
// ... 
Gawain.run {
     useRepoBuilder(
        new JDBCRepoBuilder(
            DriverManager.getConnection('jdbc:mysql://localhost:3306/database?useSSL=false&user=user&password=password'),
            new MysqlDialect()
        )
     )
    // ...
}
```

For PostgreSQL database:
```groovy
// ... 
Gawain.run {
     useRepoBuilder(
        new JDBCRepoBuilder(
            DriverManager.getConnection('jdbc:postgresql://localhost:5432/database?user=user&password=password'),
            new PostgresDialect()
        )
     )
    // ...
}
```
