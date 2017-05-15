# Microservices and data consistency (I)

As you can read in [Christian Posta's excellent article](http://blog.christianposta.com/microservices/the-hardest-part-about-microservices-data/), when designing a microservices-based solution, to solve consistency between bounded contexts our first choice will be to communicate boundaries with immutable point in time events by means of a messaging queue/listener, a dedicated event store/publish-subscribe topic or a replicated log/event processor.

¿But how to deal with situations where, inevitably, we must update data from different contexts in a single transaction either across a single database or multiple databases? A combination of JPA 2.1 unsynchronized persistence contexts, JPA Entity listeners, Kafka and [Atomikos TCC](https://www.atomikos.com/Blog/TransactionManagementAPIForRESTTCC) could fit like a glove ;-)

Let's describe that approach:





 