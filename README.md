# ArtemisOpenwireTempQueueIssue
Include the jar files from lib into your classpath.
To test the demo run SimpleServer.main() with the first argument being the connection url for the broker.
Wait until connection is set up.
Then run SimpleRequester.main() in parallel with the first argument being the connection url for the broker.
Request Reply should be performed with a reply arriving at the SimpleRequester.

Alternatively the broker URL can also be specified in the main methods directly.

# Issue
This demo runs with the activeMQ classic libraries (Openwire).
If the used broker (connection URL) is an Artemis broker we run into the following exception at the SimpleServer:
```
javax.jms.InvalidDestinationException: Cannot publish to a deleted Destination: temp-queue://ID:NTNB794-58795-1739981849636-1:1:1
	at org.apache.activemq.ActiveMQSession.send(ActiveMQSession.java:1915)
	at org.apache.activemq.ActiveMQMessageProducer.send(ActiveMQMessageProducer.java:288)
	at org.apache.activemq.ActiveMQMessageProducer.send(ActiveMQMessageProducer.java:223)
	at main.java.systema.demo.amq.SimpleServer.onMessage(SimpleServer.java:98)
	at org.apache.activemq.ActiveMQMessageConsumer.dispatch(ActiveMQMessageConsumer.java:1435)
	at org.apache.activemq.ActiveMQSessionExecutor.dispatch(ActiveMQSessionExecutor.java:131)
	at org.apache.activemq.ActiveMQSessionExecutor.iterate(ActiveMQSessionExecutor.java:202)
	at org.apache.activemq.thread.PooledTaskRunner.runTask(PooledTaskRunner.java:133)
	at org.apache.activemq.thread.PooledTaskRunner$1.run(PooledTaskRunner.java:48)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
```