package net.jotorren.microservices.forum.configuration;

import java.util.HashMap;
import java.util.Map;

import net.jotorren.microservices.context.SpringContextProvider;
import net.jotorren.microservices.tx.CompositeTransactionManager;
import net.jotorren.microservices.tx.impl.CompositeTransactionManagerKafkaImpl;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class CompositeTransactionConfiguration {
	
	@Value("${kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;

	@Value("${kafka.consumer.enable.auto.commit}")
	private String kafkaEnableAutoCommit;

	@Value("${kafka.consumer.auto.commit.interval}")
	private String kafkaAutoCommitInterval;
	
	@Value("${kafka.consumer.session.timeout}")
	private String kafkaSessionTimeout;
	
	@Value("${kafka.consumer.auto.offset.reset}")
	private String kafkaAutoOffsetReset;
	
	@Bean
	public SpringContextProvider springContextProvider(){
		return new SpringContextProvider();
	}
	
	@Bean
	public CompositeTransactionManager compositeTransactionManager() {
		return new CompositeTransactionManagerKafkaImpl();
	}

	@Bean
	public Map<String, Object> kafkaConsumerConfiguration() {
		Map<String, Object> propsMap = new HashMap<>();
		// list of host:port pairs used for establishing the initial connections to the Kakfa cluster
		propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaEnableAutoCommit);
		propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, kafkaAutoCommitInterval);
		propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaSessionTimeout);
		propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaAutoOffsetReset);
		return propsMap;
	}

	@Bean
	public Map<String, Object> kafkaProducerConfiguration() {
		Map<String, Object> props = new HashMap<>();
		// list of host:port pairs used for establishing the initial connections to the Kakfa cluster
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		return props;
	}
	
	@Bean
	public ProducerFactory<Integer, String> kafkaProducerFactory() {
		return new DefaultKafkaProducerFactory<>(kafkaProducerConfiguration());
	}
	
	@Bean
	public KafkaTemplate<Integer, String> kafkaTemplate() {
		return new KafkaTemplate<Integer, String>(kafkaProducerFactory());
	}
}
