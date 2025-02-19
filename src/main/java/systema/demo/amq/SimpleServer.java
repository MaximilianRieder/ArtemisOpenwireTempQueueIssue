/*
 * Company:   Systema GmbH
 * Copyright: (c) Copyright 2025 by Systema GmbH
 *                All Rights Reserved
 */
package main.java.systema.demo.amq;

import java.util.Date;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.command.ActiveMQQueue;

/**
 * Server answers the time of the host clock.
 * Exits gracefully when message equals "exit"
 *
 * @author Maximilian Rieder
 */
public class SimpleServer implements MessageListener
{
	ActiveMQConnectionFactory connectionFactory = null;
	ActiveMQConnection connection = null;
	ActiveMQMessageConsumer consumer = null;
	String brokerUrl;
	long ttlOnQueues = 180000;
	private ActiveMQSession serviceSession;
	private ActiveMQSession replySession;
	private ActiveMQMessageProducer replySender;

	public SimpleServer(String brokerUrl)
	{
		this.brokerUrl = brokerUrl;
	}

	public void connect() throws JMSException
	{
		connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		System.out.println("Attempt to create  connection");
		connection = (ActiveMQConnection) connectionFactory.createConnection();
		System.out.println("Connection established.");
		connection.start();
		System.out.println("Connection started.");
		serviceSession = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		replySession = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		System.out.println("Sessions created.");
	}

	private void shutdown() throws JMSException
	{
		consumer.close();
		serviceSession.close();
		replySession.close();
		System.out.println("Session closed.");
		connection.stop();
		System.out.println("Connection stopped.");
		connection.close();
		System.out.println("Connection closed.");
	}

	public void onMessage(Message message)
	{
		System.out.println("Received request");
		try
		{
			System.out.println("Destination=" + message.getJMSDestination());
			Destination replyDestination = message.getJMSReplyTo();
			System.out.println("ReplyDestination=" + replyDestination);
			if ( replyDestination == null )
			{
				System.err.println("No Reply destination cannot answer!");
				return;
			}
			if ( message instanceof TextMessage )
			{
				String messageString = ((TextMessage) message).getText();
				System.out.println("Message=" + messageString);
				if ( "exit".equals(messageString) )
				{
					shutdown();
				}
				else
				{
					String replyString = "TIME:\"" + (new Date()) + "\"";
					TextMessage jmsReply = replySession.createTextMessage();
					jmsReply.setText(replyString);
					jmsReply.setJMSReplyTo(null);
					jmsReply.setJMSDestination(replyDestination);
					replySender.send(replyDestination, jmsReply,
					                 DeliveryMode.NON_PERSISTENT,
					                 replySender.getPriority(),
					                 ttlOnQueues);
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

	public void subscribeToQ(String qname) throws JMSException
	{
		ActiveMQQueue queue = (ActiveMQQueue) serviceSession.createQueue(qname);
		consumer = (ActiveMQMessageConsumer) serviceSession.createConsumer(queue);
		replySender = (ActiveMQMessageProducer) replySession.createProducer(null);
		consumer.setMessageListener(this);
		System.out.println("serving on:" + queue);
	}

	public static void main(String[] args) throws JMSException
	{
		String brokerUrl = "tcp://test-server:6666";
		if ( args.length > 0 )
		{
			brokerUrl = args[0];
		}
		SimpleServer server = new SimpleServer(brokerUrl);
		server.connect();
		server.subscribeToQ("this.is.a.test.queue");
	}
}
