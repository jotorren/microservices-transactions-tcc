package net.jotorren.microservices.tx.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import net.jotorren.microservices.tx.CompositeTransactionManager;
import net.jotorren.microservices.tx.EntityCommand;
import net.jotorren.microservices.tx.Serializer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.kafka.core.KafkaTemplate;

public class CompositeTransactionManagerKafkaImpl implements CompositeTransactionManager {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeTransactionManager.class);

	@Value("${zookeeper.partitions:0}")
	private int zooPartitions;
	
	@Value("${zookeeper.replication:0}")
	private int zooReplication;
	
	@Value("${kafka.consumer.poll.timeout:0}")
	private long kafkaConsumerPollTimeout;
	
	
	@Autowired(required = false)
	private ZkUtils zkUtils;
	
	@Autowired(required = false)
	private KafkaTemplate<Integer, String> kafkaTemplate;

	@Autowired(required = false)
	@Qualifier("kafkaConsumerConfiguration")
	private Map<String, Object> configuration;
	
	@Autowired(required = false)
	private Serializer<EntityCommand<?>> serializer;
	
	@Override
	public void open(String txId) {
		// Add topic configuration here
        Properties topicConfig = new Properties();

        AdminUtils.createTopic(zkUtils, txId, zooPartitions, zooReplication, topicConfig, RackAwareMode.Enforced$.MODULE$);
	}

	@Override
	public void enlist(String txId, EntityCommand<?> command) {
		kafkaTemplate.send(txId, serializer.writeToString(command));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EntityCommand<?>> fetch(String txId) {
		List<EntityCommand<?>> transactionOperations = new ArrayList<EntityCommand<?>>();

		Map<String, Object> consumerConfigs = (Map<String, Object>)configuration.get("kafkaConsumerConfiguration");
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
		
		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(consumerConfigs);
		kafkaConsumer.subscribe(Arrays.asList(txId));
		
		ConsumerRecords<String, String> records = kafkaConsumer.poll(kafkaConsumerPollTimeout);
		for (ConsumerRecord<String, String> record : records){
			LOG.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), record.value());
			try {
				transactionOperations.add(serializer.readFromString(record.value()));
			} catch (SerializationFailedException e) {
				LOG.error("Unable to deserialize [{}] because of: {}", record.value(), e.getMessage());
			}
		}
		
		kafkaConsumer.close();
			
		return transactionOperations;
	}
	
	@Override
	public void close(String txId) {
		AdminUtils.deleteTopic(zkUtils, txId);
	}
}
