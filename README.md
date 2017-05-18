# Microservices and data consistency (I)

As you can read in [Christian Posta's excellent article](http://blog.christianposta.com/microservices/the-hardest-part-about-microservices-data/), when designing a microservices-based solution **our first choice to solve consistency between bounded contexts will be to communicate boundaries with immutable point in time events** (by means of a messaging queue/listener, a dedicated event store/publish-subscribe topic or a database/replicated log/event processor).

¿But how to deal with situations where, inevitably, we must update data from different contexts in a single transaction either across a single database or multiple databases? A combination of JPA 2.1 unsynchronized persistence contexts, JPA Entity listeners, Kafka and [Atomikos TCC](https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC) could fit like a glove ;-) 

Let's describe that approach. We will start by introducing all the actors:

- **Domain Services**. Each of the stateless and autonomous pieces that the whole system has been divided into.
- **Composite Services**. Coarse-grained service operations which are composed by many calls to one or more domain services.
- **Command**. Data describing a persistence operation performed by a domain service: "*an operation on a given entity within certain context*"
- **Composite transaction**. Set of commands that must be grouped and carried out together.
- **Coordinator**. Service to manage composite transactions lifecycle, deciding whether or not changes (commands) must be applied to the corresponding underlying repositories.
- **TCC Service**. *Try*-*Cancel*/*Confirm* protocol implementation. It handles all TCC remote calls verifying no transaction timeout has been exceeded.
- **Distributed, replicated event log**. Distributed store of composite transactions accessible by any service instance (domain, composite or coordinator)

I would like to point out that Domain, Composite, Coordinator and TCC services have no 2PC/XA support and they can be dynamically allocated/destroyed.



Regarding the sequence of actions:

1. A client makes a remote call to a composite service
2. The composite service knows which domain services needs to invoke and passes that information to the coordinator
3. The coordinator creates a composite transaction or, in other words, a persistent topic for each domain service involved in the operation. Every topic will be uniquely identified by a string that can be interpreted as a *partial transaction id* (partial because a topic will store only commands for instances of a single domain service)
4. The composite service calls each domain service using its respective *partial transaction id*
5. A domain service performs persistence operations through a JPA unsynchronized persistence context and publishes appropriate commands to the topic identified by the given *partial transaction id*

![producers](https://cloud.githubusercontent.com/assets/22961359/26069317/baa16904-39a0-11e7-91bd-b2d3bd75cf32.png)



1. If all domain services calls succeed, the composite service signals the coordinator to commit the changes
   - The coordinator calls the confirm operation on the TCC service
   - The TCC service calls the confirm operation on each domain service passing the correct *partial transaction id*
   - Each domain service reads all commands from the given topic, executes them through a JPA unsynchronized persistence context and finally applies the derived changes to the underlying repository.
   - If all commit calls succeed the business operation ends successfully, otherwise the operation ends with an heuristic failure
2. If a domain service call fails, the composite service signals the coordinator to rollback the changes
   - The coordinator calls the cancel operation on the TCC service
   - The TCC service calls the cancel operation on each domain service passing the correct *partial transaction id*
   - The business operation ends with error

![consumers](https://cloud.githubusercontent.com/assets/22961359/26069329/c3b944da-39a0-11e7-8916-a29e4df2e124.png)



## Build

```shell
# clone this repo
# --depth 1 removes all but one .git commit history

git clone --depth 1 https://github.com/jotorren/microservices-transactions-tcc.git my-project

# change directory to your project
cd my-project

# build artifacts
mvn clean install
```



## Run

First of all you must download and install Zookeeper & Kafka servers. Please follow guidelines described in:

- https://zookeeper.apache.org/doc/r3.1.2/zookeeperStarted.html
- https://kafka.apache.org/quickstart

Once both servers are up and running you can start all services:

- Composite service to create source code items and discussion boards + TCC Service

```shell
# inside your project home folder
cd rahub-composite-service
mvn spring-boot:run
# default port 8090
```

- Domain service to create/query pieces of source code


```shell
# inside your project home folder
cd rahub-source-code-service
mvn spring-boot:run
# default port 8091
```

- Domain service to create/query discussion boards about source code items

```shell
# inside your project home folder
cd rahub-forum-service
mvn spring-boot:run
# default port 8092
```



## Available services

- `/api`: http://localhost:8090/api/api-docs?url=/api/swagger.json

![composite65](https://cloud.githubusercontent.com/assets/22961359/26103358/4ccbd47a-3a39-11e7-9eb9-8810d4efe123.png) 



- `/api/coordinator`: http://localhost:8090/api/api-docs?url=/swagger-tcc.json

In the current example TCC service runs on the same JAX-RS container as the composite does, but it will be preferable to deploy it on its own instance.

![tcc-ops65](https://cloud.githubusercontent.com/assets/22961359/26151969/5c16e894-3b05-11e7-9e33-519ea8c3d9a8.png) 



- `/content`: http://localhost:8091/index.html?url=/content/swagger.json


![sourcecode65](https://cloud.githubusercontent.com/assets/22961359/26103359/4cce7978-3a39-11e7-82c3-baa7f9024696.png) 



- `/forum`: http://localhost:8092/index.html?url=/forum/swagger.json


![forum65](https://cloud.githubusercontent.com/assets/22961359/26103360/4cd31258-3a39-11e7-9624-c100d0622a5c.png) 



## Considerations

#### REST implementation

Source code and forum services use Jersey whilst composite and TCC services rely on CXF. With regard to swagger ui, the former contain required static resources inside `src/main/resources/static` while the latter only depend on a [webjar](http://www.webjars.org/) and have an empty static folder.

#### Repositories

Source code and forum services use an embedded H2 file based database. You can check the configuration looking at their respective `src/main/resources/application.properties`. By default, both data models are initialized on startup, but that behavior can be disabled  by uncommenting the following lines:

```properties
#spring.jpa.generate-ddl: false
#spring.jpa.hibernate.ddl-auto: none
```

Additionally, H2 web console is enabled in both cases and can be accessed through the URI `/h2/console`.



## Components

![Core classes](https://cloud.githubusercontent.com/assets/22961359/26158987/ae0acd88-3b1d-11e7-85a1-68ba872a3867.png)

Pink classes are provided by [Atomikos](https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC) and contain the TCC protocol implementation. Green ones are generic and reusable components to isolate and hide the complexity of composite transactions management. 



## Key aspects

#### 1. Transactional persistence operations: unsynchronized persistence contexts

Persistence operations executed inside a Composite Transaction are delegated to *unsynchronized entity manager*s: you can create, change and delete entities without doing any change to the repository until you force the manager to join an existent `LOCAL/JTA` transaction (note the `@Transactional` annotation present in the `commit()` method ).

```java
@Repository
@Scope("prototype")
public class CompositeTransactionParticipantDao {

	@PersistenceContext(type = PersistenceContextType.EXTENDED, 
                        synchronization = SynchronizationType.UNSYNCHRONIZED)
	private EntityManager em;

  	@Transactional(readOnly=false)
  	public void commit() {
		em.joinTransaction();
	}
  
	public void save(Object entity) {
		em.persist(entity);
	}

	public <T> T saveOrUpdate(T entity) {
		return em.merge(entity);
	}

	public void remove(Object entity) {
		em.remove(entity);
	}
  
    public <T> T findOne(Class<T> entityClass, Object pk){
    	return getEntityManager().find(entityClass, pk);
    }
}
```

As stated in [Spring ORM documentation](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/orm.html): 

> `PersistenceContextType.EXTENDED` is a completely different affair: This results in a so-called extended EntityManager, which is *not thread-safe* and hence must not be used in a concurrently accessed component such as a Spring-managed singleton bean

This is the reason why we set `prototype` as scope for any `DAO` with an *unsynchronized persistence context* injected into it.

And some final aspects to be aware of:

Any call to the `executeUpdate()` method of a `Query` created through an *unsynchronized entity manager* will fail reporting `javax.persistence.TransactionRequiredException: Executing an update/delete query`. Consequently, bulk update/delete operations are not supported.

On the other hand, it is possible to create/execute a `Query` to look for data but, in that case, only already persisted (committed) entries are searchable. If you want to retrieve entities that have not yet been saved (committed) you must use `EntityManager` `find()` methods.

Keep in mind that any repository constraint will be checked only when the `EntityManager` joins the transaction (that is during the *commit* phase). Therefore it will be preferable to implement as many validations as possible out of the repositories. In doing so, we can detect potential problems in a very early stage, increasing the overall performance and consistency of the system.



#### 2. From persistence operation to Command: JPA entity listeners and callback methods

*Default entity listeners* are listeners that should be applied to all entity classes. Currently, they can only be specified in a mapping XML that can be found in `src/main/resources/META-INF/orm.xml`

*Callback* methods are user defined methods that are attached to entity lifecycle events and are invoked automatically by JPA when these events occur:

- `@PrePersist` - before a new entity is persisted (added to the `EntityManager`).
- `@PostPersist` - after storing a new entity in the database (during *commit* or *flush*).
- `@PostLoad` - after an entity has been retrieved from the database.
- `@PreUpdate` - when an entity is identified as modified by the `EntityManager`.
- `@PostUpdate` - after updating an entity in the database (during *commit* or *flush*).
- `@PreRemove` - when an entity is marked for removal in the `EntityManager`.
- `@PostRemove` - after deleting an entity from the database (during *commit* or *flush*).

(For further details see http://www.objectdb.com/java/jpa/persistence/event)

If we want to find out which entities have been created, updated or removed through an *unsynchronized entity manager*, we only need *@Pre\* callback* methods:  

```java
public class ChangeStateJpaListener {

	@PrePersist
	void onPrePersist(Object o) {
		enlist(o, EntityCommand.Action.INSERT);
	}

	@PreUpdate
	void onPreUpdate(Object o) {
		enlist(o, EntityCommand.Action.UPDATE);
	}

	@PreRemove
	void onPreRemove(Object o) {
		enlist(o, EntityCommand.Action.DELETE);
	}
  
	private void enlist(Object entity, EntityCommand.Action action){
		EntityCommand<Object> command = new EntityCommand<Object>();
		command.setEntity(entity);
		command.setAction(action);
		command.setTimestamp(System.currentTimeMillis());
		// send command to some store/queue
	}
}
```



#### 3. Commands persistence and distribution

At this point we know how persistence operations from a given Domain Service are translated into Commands, but once generated we need to save and distribute them to all available instances of that service. This is accomplished by using Kafka persistent topics. Let's have a deeper look at the proposed mechanism:

When a Composite Service asks the Coordinator (`TccRestCoordinator`) to open a new Composite Transaction, the first thing the latter does is to generate an UUID to uniquely identify that transaction. Then it creates as many topics as different Domain Services must be included, assigning them a name resulting from concatenating the UUID and an internal sequence number (building the so-called *partial transaction id*). Once all resources have been allocated, it returns to the Composite Service a `CompositeTransaction` object that includes the transaction global UUID and all the partial transaction ids. From this moment on, any call dispatched by the Composite Service to a Domain Service will always include the corresponding partial transaction id (as an extra `@PathParam`)

Furthermore, the JPA entity listener responsible for generating Commands (see point #2) requires the name of the topic to use for publishing them (after a proper serialization process has been applied). How can that standard JPA class obtain a value available inside an `Spring` bean? `ThreadLocal` variables come to the rescue: just before a Domain Service makes the first call to a `DAO`, it adds its partial transaction id to a `ThreadLocal` variable. Because of JPA listeners run in the same thread as the `EntityManager` operation they have access to any  `ThreadLocal` variable created by the service and can retrieve the partial transaction id from it. Finally, a `org.springframework.kafka.core.KafkaTemplate` instance is used to send the `JSON` representation of the Command to the appropriate topic.



#### 4. From Command to persistence operation: inherited method from `CompositeTransactionParticipantDao`

Because an `EntityCommand` object contains the entity to create/update/delete and the action to apply to it, it's very straightforward to find out which persistence operation must be executed by a given `EntityManager`; this is as simple as adding an special method to the generic `CompositeTransactionParticipantDao` where the`EntityManager` is injected:

```java
public void apply(List<EntityCommand<?>> transactionOperations) {
	if (null == transactionOperations) {
		return;
	}

	for (EntityCommand<?> command : transactionOperations) {
		switch (command.getAction().ordinal()) {
		case 0:
			save(command.getEntity());
			break;
		case 1:
			saveOrUpdate(command.getEntity());
			break;
		case 2:
			remove(command.getEntity());
			break;
		}
	}
}
```



#### 5. Composite Transaction lifecycle

[01] A Composite Service asks the Coordinator  (`TccRestCoordinator`) to open a new Composite Transaction. The call includes the maximum amount of time (in milliseconds) to complete the transaction and the URL of each participant (Domain Service) to be used when cancelling/confirming its operations (as specified by the TCC protocol).

```java
CompositeTransaction transaction = tccRestCoordinator.open(transactionTimeout, featureAbcTccUrl, 
		featureXyzTccUrl);
```

[02] The Coordinator generates the Composite Transaction UUID. Then, for each participant it computes the partial transaction id and uses a `CompositeTransactionManager` instance (provided by the Spring container) to initialize the transaction persistence/distribution (with the Kafka-based implementation a persistent topic is created)

[03] The Composite Service starts calling each Domain Service and processes their responses

[04] When a Domain Service receives a call, it extracts the transaction partial id from the URI

```java
public Response txedOperation(@Context UriInfo uriInfo, @PathParam("txid") String txid, Feature data)
```

 [05] Defines a `ThreadLocal` variable and sets its value to the transaction partial id

```java
ThreadLocalContext.put(CURRENT_TRANSACTION_KEY, txId);
```

[06] Asks Spring container to return a **NEW** instance of a `DAO` with an *unsynchronized* `EntityManager` injected into it. Makes some calls to `DAO` methods

[07] The `DAO` translates each method call to a set of persistence operations, delegating all of them to its `EntityManager`

[08] For every persistence operation, the JPA container executes the global entity listener (in the same thread as the `EntityManager` operation)

[09] The JPA listener checks if a partial transaction id has been informed by the service and in case of unavailability, it does nothing. Otherwise (when a partial id can be positively found) it creates a new `EntityCommand` instance with the entity, the type of operation, the partial transaction id and a timestamp. After that, it uses the `CompositeTransactionManager` instance (provided by the Spring container) to "enlist" the Command.

```java
private void enlist(Object entity, EntityCommand.Action action, String txId){
	
	EntityCommand<Object> command = new EntityCommand<Object>();
	command.setEntity(entity);
	command.setAction(action);
	command.setTransactionId(txId);
	command.setTimestamp(System.currentTimeMillis());
	
	CompositeTransactionManager txManager = 
		SpringContext.getBean(CompositeTransactionManager.class);
	txManager.enlist(txId, command);
}
```

[10] When using the Kafka-based implementation of  `CompositeTransactionManager`, the `EntityCommand` object is serialized to a `JSON` string before storing it in the appropriate topic.



So far, we have completed the *Try* part of the *Try*-*Cancel*/*Confirm* protocol. What about the *Cancel*/*Confirm* one? Let's start with *Confirm*



[11] Once the Composite Service ends calling Domain Services, it invokes the `commit()` method on the Coordinator  (`TccRestCoordinator`) 

[12] The coordinator sends a PUT request to the confirm URI of the TCC Service, adding the Composite Transaction data as the request content

[13] The TCC Service iterates over the transaction participants list and, for each of them, sends a PUT request to their respective TCC confirm URI (computed during the Composite Transaction creation)

[14] When a Domain Service receives the confirm call, it extracts the transaction partial id from the URI

```java
public void confirm(@PathParam("txid") String txid)
```

[15] Uses the  `CompositeTransactionManager` instance provided by the Spring container to get all the Commands "enlisted" in that  partial transaction

[16] Asks the Spring container to return a **NEW** instance of a `DAO` with an *unsynchronized* `EntityManager` injected into it.

[17] Invokes the `apply()` method on the `DAO` to translate the list of Commands to persistence operations. Because of we're applying already persisted commands, we must "disable" the JPA global entity listener. This can be easily done by ensuring no `ThreadLocal` variable with the partial id has been defined.

[18] Forces the `DAO` to join a `LOCAL/JTA` transaction and, thus, all persistence operations are applied to the underlying repository.

[19] When a confirm call from a Domain Service fails, a 404 response is returned. Once the TCC Service receives it, the confirmation process is stopped and a 409 response is sent back to the Coordinator who in turn propagates that value to the Composite Service.

[20] If all confirm calls succeed (all return 204) the TCC Service also responds with a 204 to the Coordinator who in turn propagates that value to the Composite Service.



And finally the *Cancel* branch:



[11] If Composite Service detects some error condition, it can abort the Composite Transaction by invoking the `rollback()` method on the Coordinator  (`TccRestCoordinator`) 

[12] In that case, the coordinator sends a PUT request to the cancel URI of the TCC Service, adding the Composite Transaction data as the request content

[13] The TCC Service iterates over the transaction participants list and, for each of them, sends a PUT request to their respective TCC cancel URI (computed during the Composite Transaction creation)

[14] When a Domain Service receives the cancel call, it extracts the transaction partial id from the URI

```java
public void cancel(@PathParam("txid") String txid)
```

[15] In the current implementation the Domain Service does nothing. Perhaps a valid action could be to "close" the partial transaction (when using the Kafka-based implementation of the  `CompositeTransactionManager` that could trigger a topic removal)

[16] When a cancel call from a Domain Service fails, a 404 response is returned. Once the TCC Service receives it, a log trace is written and the cancellation process goes on. When the last call finishes, the TCC Service returns a 204 response to the Coordinator who in turn propagates that value to the Composite Service.

[17] If all cancel calls succeed (all return 204) the TCC Service also responds with a 204 to the Coordinator who in turn propagates that value to the Composite Service.