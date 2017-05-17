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

#### Unsynchronized persistence contexts

Persistence operations executed inside a composite transaction are delegated to *unsynchronized entity manager*s: you can create, change and delete entities without doing any change to the repository until you force the manager to join an existent `LOCAL/JTA` transaction (note the `@Transactional` annotation added to the `commit()` method ).

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

Keep in mind that any repository constraint will be checked only when the entity manager joins the transaction (that is during the *commit* phase). Therefore it will be preferable to implement as many validations as possible out of the repositories. In doing so, we can detect potential problems in a very early stage, increasing the overall performance and consistency. 

#### JPA entity listeners and callback methods

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

#### Commands persistence and distribution

(pending)

#### Committing changes

(pending)

#### Composite transaction IDs

(pending)