/*
 * Company:   Systema GmbH
 * Copyright: (c) Copyright 2025 by Systema GmbH
 *                All Rights Reserved
 */
package main.java.systema.demo.amq;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQQueueReceiver;
import org.apache.activemq.ActiveMQQueueSender;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;

/**
 * Sends a request on a queue specifying a temp. queue as reply destination.
 *
 * @author Maximilian Rieder
 */
public class SimpleRequester implements MessageListener
{
	ActiveMQConnectionFactory connectionFactory = null;
	ActiveMQConnection connection = null;
	long ttlOnQueues = 180000;
	private int priority = 0;
	private int deliverymode = DeliveryMode.NON_PERSISTENT;
	private ActiveMQSession requestSession;
	private ActiveMQSession replySession;
	private ActiveMQQueueSender qSender = null;
	private ActiveMQQueueReceiver qReceiver = null;
	private final Object stoplight = new Object();
	private String reply = null;
	private String brokerUrl;

	public SimpleRequester(String brokerUrl)
	{
		this.brokerUrl = brokerUrl;
	}

	public void connect() throws JMSException
	{
		System.out.println("Attempt to create authentication disabled connection factory.");
		connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		System.out.println("Attempt to create  connection");
		connection = (ActiveMQConnection) connectionFactory.createConnection();
		RedeliveryPolicy redeliveryPolicy = connection.getRedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(0);
		System.out.println("Connection established.");
		connection.start();
		System.out.println("Connection started.");
		requestSession = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		replySession = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		System.out.println("Sessions created.");
		qSender = (ActiveMQQueueSender) requestSession.createSender(null);
	}

	private void shutdown() throws JMSException
	{
		requestSession.close();
		replySession.close();
		System.out.println("Session closed.");
		connection.stop();
		System.out.println("Connection stopped.");
		connection.close();
		System.out.println("Connection closed.");
	}

	public void onMessage(Message message)
	{
		try
		{
			System.out.println("Received on:" + message.getJMSDestination());
			if ( message instanceof TextMessage )
			{
				reply = ((TextMessage) message).getText();
				synchronized(stoplight)
				{
					stoplight.notifyAll();
				}
			}
			else
			{
				System.err.println("No TextMessage:" + message);
			}
		}
		catch ( JMSException e )
		{
			e.printStackTrace();
		}
	}

	public void request(String subject, String message, final long timeout) throws JMSException
	{
		synchronized(this.stoplight)
		{
			ActiveMQQueue serviceQueue = (ActiveMQQueue) requestSession.createQueue(subject);
			TemporaryQueue rcvQueue = replySession.createTemporaryQueue();
			qReceiver = (ActiveMQQueueReceiver) replySession.createReceiver(rcvQueue);
			qReceiver.setMessageListener(this);
			reply = null;
			TextMessage request = requestSession.createTextMessage();
			request.setText(message);
			request.setJMSReplyTo(rcvQueue);
			qSender.send(serviceQueue, request, deliverymode, priority, ttlOnQueues);
			System.out.println("Request sent:" + request);
			try
			{
				stoplight.wait(timeout);
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
		if ( reply == null )
		{
			System.err.println("There was a timeout");
		}
		else
		{
			System.out.println("The reply was:" + reply);
		}
	}

	public static void main(String[] args) throws JMSException
	{
		String brokerUrl = "tcp://test-server:6666";
		if ( args.length > 0 )
		{
			brokerUrl = args[0];
		}
		SimpleRequester client = new SimpleRequester(brokerUrl);
		client.connect();
		client.request("this.is.a.test.queue", "getServerTime", 5000);
		client.shutdown();
	}
}
