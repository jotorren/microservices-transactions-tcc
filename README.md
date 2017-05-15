# Microservices and data consistency (I)

As you can read in [Christian Posta's excellent article](http://blog.christianposta.com/microservices/the-hardest-part-about-microservices-data/), when designing a microservices-based solution (in order to solve consistency between bounded contexts) **our first choice will be to communicate boundaries with immutable point in time events** by means of a messaging queue/listener, a dedicated event store/publish-subscribe topic or a replicated log/event processor.

¿But how to deal with situations where, inevitably, we must update data from different contexts in a single transaction either across a single database or multiple databases? A combination of JPA 2.1 unsynchronized persistence contexts, JPA Entity listeners, Kafka and [Atomikos TCC](https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC) could fit like a glove ;-) 

Let's describe that approach. We will start by introducing all the actors:

- **Domain Services**. Each of the stateless and autonomous pieces that the whole system has been divided into.
- **Composite Services**. Coarse-grained services operations which are composed by many calls to one or more domain services.
- **Command**. Data describing a persistence operation performed by a domain service: "*an operation on a given entity within certain context*"
- **Composite transaction**. Set of commands that must be grouped and carried out together.
- **Coordinator**. Service to manage composite transactions lifecycle, deciding whether or not their changes (commands) must be applied to the corresponding underlying repositories.
- **Persistent Messaging System**. Distributed store of composite transactions accessible by any service instance (domain, composite or coordinator)

I would like to point out that Domain, Composite and Coordinator services have no 2PC/XA support and they can be dynamically allocated/destroyed.



And now the sequence of actions:

1. A client makes a remote call to a composite service
2. The composite service knows which domain services needs to invoke and passes that information to the coordinator
3. The coordinator creates a composite transaction or, in other words, a persistent topic for each domain service involved in the operation. Every topic will be uniquely identified by a string that can be interpreted as a *partial transaction id* (partial because a topic will store only commands for instances of a single domain service)
4. The composite service calls each domain service using its respective *partial transaction id*
5. A domain service performs persistence operations through a JPA unsynchronized persistence context and publishes appropriate commands to the topic identified by the given *partial transaction id*
6. If all domain services calls succeed, the composite service signals the coordinator to confirm the changes
   - The coordinator calls the commit operation on each domain service passing the correct *partial transaction id*
   - Each domain service reads all commands from the corresponding topic, executes them through a JPA unsynchronized persistence context and finally applies the derived changes to the underlying repository.
   - If all commit calls succeed the business operation ends successfully, otherwise a rollback call is propagated
7. If a domain service call fails, the composite service signals the coordinator to cancel the changes
   - The coordinator calls the rollback operation on each domain service passing the correct *partial transaction id*
   - The business operation ends with error









 