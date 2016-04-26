# Gawain
<img src="https://farm9.staticflickr.com/8425/7663862222_2ebf44e5e8_o.jpg" width="150px"/> <br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.smecsia.gawain/gawain/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/me.smecsia.gawain/gawain)<br/>
Microframework focusing on data processing and aggregation in distributed environment

## Features

* Distributed processing and aggregation with the usage of many threads/processes
* Aggregation data is stored into different storages (e.g. databases)
* Messages broadcasting within cluster
* Distributed pessimistic locking for each aggregation key during aggregation
* Distributed scheduler, that can perform tasks on every node or just on the master node

## Setup
build.gradle
```groovy
    compile 'ru.qatools:gawain:0.1.4'
```

## Usage

Gawain is an event-driven framework which operates with the terms like `queue`, `event`, `state`, `processor`, `aggregator`, `repository`, `broadcaster`, `timer`. 
* `event` is a message that can affect system. It is enqueued into the `queue` and later is processed by `processor` or `aggregator`.
* `queue` is a storage, where messages reside until they are processed by `processor` or `aggregator`.
* Unlike the `queue`, `broadcaster` is a message broker that routes messages to every node of the cluster.
* `processor` - is a stateless lambda, which can modify event and then route it to the next `processor` or `aggregator`.
* `aggregator` - is a stateful `processor`, which holds the state consistently as a [distributed map](http://en.wikipedia.org/wiki/Distributed_hash_table) 
of [correlation identifiers](http://camel.apache.org/correlation-identifier.html) and values. Aggregation can happen 
on any node of the cluster and thus must be performed concurrently. Gawain uses the 
[distributed locks](http://en.wikipedia.org/wiki/Distributed_lock_manager) for that purpose.
* `state` represents the single value identified by aggregation key and can contain any accumulated data. 
* `repository` is a storage for states. It can be in-memory or can represent the distributed map of values.
* `timer` is a scheduled job, which can perform any operations periodically with given schedule.

The example:
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

// when processing is completed the following line will print 
// "Mr. Petrov, Mr. Sidorov, Mrs. Ivanova, Mrs. Petrova"
println(gawain.repo('people').keys())
```

### Cluster

Gawain's main purpose is to handle processing and aggregation jobs, to process the queues of messages and then to store aggregation results 
into database. All the operations can be performed in parallel on different nodes of the cluster. Usually all cluster nodes are euqal. 
There is no "Master" node for processing and aggregation. The exception is the distributed scheduler that can be performed on master node only.  
Besides that, nodes may be configured differently (for example, they can have different count of queue consumers, depending on the resources (e.g. CPU/Memory)). 
All nodes may be (or not) connected to a single instance of message queue broker (e.g. ActiveMQ) or to a single data storage (e.g. MongoDB/Relational database).

### Routing

Route can represent a graph, in which each vertex is a single processor or aggregator. It can be cyclic or acyclic.
Typical route looks like a sequence of processors which ends with aggregator: 
```
proc1 -> proc2 -> ... -> procN -> aggregator
```
This allows to transform the source event and then accumulate the transformed events within repository.

```groovy
processor('proc1', { it * 2 }).to('proc2')
processor('proc2', { it - 1 }).to('proc3')
processor('proc3', { it / 2 }).to('aggregator')
aggregator('aggregator')
```

The code above allows to perform some calculations on values and then store them within the repository. 
For example, if we send value `2` to `proc1` then repository for `aggregator` will contain value `1.5`.

Route can be conditional or unconditional. To specify a condition, you can use `to` or `broadcast` 
methods directly from processor being based on message value:

```groovy
processor 'router', { evt -> 
    switch (evt) {
        case String: to('strings', evt); break;
        case Integer: to('integers', evt); break;
        default: to('trash', evt); break;
    }
}
```

* If event is sent from one processor to another via `to` method, then the next processing will occur on a single node of 
the cluster. If you configure the distributed message broker (e.g. ActiveMQ), the target node may be any of the cluster nodes, 
otherwise all queues will be in-memory (and thus processing will occur on the same node as the previous one).
* If event is sent from one processor to another via `broadcast` method, and there is a configured distributed broadcasting 
broker (e.g. ActiveMQ or MongoDB), then all the nodes of the cluster will receive such message. And it will be processed by the 
next processor on every cluster node (one time for each node).

```groovy
processor('launcher').broadcast('launch')
processor 'launch', { println('Launching command ${it} on every cluster node...') }
```

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

### Usage from java code

Gawain easily integrates with java code. The main difference is due to the difference in Java lambdas and Groovy closures. 
In Java you have to use the router reference within lambdas:

```java
final List<User> users = new ArrayList();

final Gawain gawain = Gawain.run(r -> {
    r.processor("male",
            filter(evt -> !"Johnson".equals(evt)),
            process(evt -> "Mr. " + evt)).to("users");
    r.processor("female", process(evt -> "Mrs. " + evt)).to("users");
    r.processor("users", process(User::new)).to("output");
    r.processor("output", process(evt -> users.add((User) evt)));
});

gawain.to("female", "Ivanova");
gawain.to("male", "Johnson");
gawain.to("male", "Petrov");
// ...
// after processing is finished users will contain ["Mrs. Ivanova", "Mr. Petrov"]

```

### Processors & Aggregators options

You can specify a number of options for processors and aggregators:
* `consumers` - Limits the queue consumers count. Specifies the number of concurrently performing consumers. 
Default value: 1.
* `bcConsumers` - Limits the number of concurrently performing consumers for broadcasted messages. 
Default value: 1.
* `processors` - Processing thread pool size. Sets the maximum processing threads for incoming messages.
This option specifies the number of concurrently processing messages per consumer. 
Default value: 10.
* `maxLockWaitMs` - Specifies maximum time in which the lock must be obtained. This option allows to prevent the 
deadlocks when one of the consumers is locked for a long time.
Default value: 30000.
* `lockPollIntervalMs` - Allows to configure the interval of polling the database to acquire the lock. Lower values 
lead to the higher loads of the database. Higher values may lead to the slower aggregation. 
Default value: 10
* `maxQueueSize` - Sets the maximum queue size. With very intensive messages stream consumers
sometimes cannot handle all of them. This option allows to limit the maximum messages within queue. 
Newer messages will be dropped if queue is full. This option may lead to inconsistency. It is disabled
by default.
* `stateSerializer` - Allows to set the serializer for the state
* `messageSerializer` - Allows to set the serializer for the events

### Setting the default options

```groovy
// ... 
Gawain.run {
    // overriding the default options
    defaultOpts(stateSerializer: new JacksonStateSerializer())
    // defining your own scheduler implementation
    useScheduler(new MySchedulerImplementation())
    // defining your own queue builder
    useQueueBuilder(new MyQueueBuilder())
    // defining your own thread pool builder
    useThreadPoolBuilder(new MyThreadPoolBuilder())
    // defining the behaviour when unknown queue name appears in route
    // if false no exception is raised
    failOnMissingQueue(false)
    // ...
}
```



### ActiveMQ as a message broker and broadcaster

build.gradle
```groovy
compile 'ru.qatools:gawain-activemq:1.3'
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
compile 'ru.qatools:gawain-mongodb:1.3'
compile 'ru.qatools:gawain-jackson:1.3'
```

Somewhere in your code:
```groovy
// ... 
def dbURL = 'mongodb://user:password@localhost:27017/database?w=majority'
def dbName = 'database'
Gawain.run {
    // setting Jackson as a serializer of the states
    defaultOpts(stateSerializer: new JacksonStateSerializer())
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
compile 'ru.qatools:gawain-jdbc:1.3'
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
