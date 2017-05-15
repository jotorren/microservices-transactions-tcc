# Microservices and data consistency (I)

As you can read in [Christian Posta's excellent article](http://blog.christianposta.com/microservices/the-hardest-part-about-microservices-data/), when designing a microservices-based solution, to solve consistency between bounded contexts **our first choice will be to communicate boundaries with immutable point in time events** by means of a messaging queue/listener, a dedicated event store/publish-subscribe topic or a replicated log/event processor.

¿But how to deal with situations where, inevitably, we must update data from different contexts in a single transaction either across a single database or multiple databases? A combination of JPA 2.1 unsynchronized persistence contexts, JPA Entity listeners, Kafka and [Atomikos TCC](https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC) could fit like a glove ;-) 

Let's describe that approach. We will start by introducing all the actors:

- **Context Services**. Each of the stateless and autonomous pieces that the whole system has been divided into.
- **Composite Services**. Coarse-grained services operations which are composed by many calls to one or more context services.
- **Command**. Data describing a persistence operation performed by any context service: "*that operation on a given entity inside certain environment*"
- **Composite transaction**. Set of commands that must be grouped and carried out together.
- **Coordinator**. Service to manage the lifecycle of any composite transaction, deciding whether or not its changes (commands) must be applied to the corresponding underlying repositories.
- **Persistent Messaging System**. Distributed store of composite transactions accessible by any service instance (context, composite or coordinator)

I would like to point out that Context, Composite and Coordinator services have no 2PC/XA support and they can be dynamically allocated/destroyed.







 