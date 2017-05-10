package net.jotorren.microservices.configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import net.jotorren.microservices.tx.CompositeTransactionManager;
import net.jotorren.microservices.tx.impl.CompositeTransactionManagerKafkaImpl;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomikos.icatch.tcc.rest.CoordinatorImp;
import com.atomikos.icatch.tcc.rest.TransactionProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

@Configuration
public class TccRestConfiguration {

	@Value("${tcc.rest.coordinator.base.url}")
	private String tccCoordinatorBaseUrl;

	
	@Value("${zookeeper.servers}")
	private String zooServers;

	@Value("${zookeeper.session.timeout}")
	private int zooSessionTimeout;
	
	@Value("${zookeeper.connection.timeout}")
	private int zooConnectionTimeout;

	
//	@Value("${kafka.bootstrap-servers}")
//	private String kafkaBootstrapServers;
//
//	@Value("${kafka.consumer.enable.auto.commit}")
//	private boolean kafkaEnableAutoCommit;
//
//	@Value("${kafka.consumer.auto.commit.interval}")
//	private long kafkaAutoCommitInterval;
//	
//	@Value("${kafka.consumer.session.timeout}")
//	private long kafkaSessionTimeout;
//	
//	@Value("${kafka.consumer.auto.offset.reset}")
//	private String kafkaAutoOffsetReset;
	
    @Bean
    public CoordinatorImp tccCoordinatorService() {
        return new CoordinatorImp();
    }
    
	@Bean
	public WebTarget tccCoordinatorClient() {
		Client client = ClientBuilder.newClient();
		client.register(new JacksonJaxbJsonProvider());
		client.register(new TransactionProvider());
		WebTarget target = client.target(tccCoordinatorBaseUrl);
		return target.path("/coordinator");
	}

	@Bean
	public CompositeTransactionManager compositeTransactionManager() {
		return new CompositeTransactionManagerKafkaImpl();
	}
	
	@Bean
	public ZkClient zkClient() {
		return new ZkClient(zooServers, zooSessionTimeout, zooConnectionTimeout, ZKStringSerializer$.MODULE$);
	}
	
	@Bean
	public ZkUtils zkUtils() {
        // Security for Kafka was added in Kafka 0.9.0.0
        boolean isSecureKafkaCluster = false;		
		return new ZkUtils(zkClient(), new ZkConnection(zooServers), isSecureKafkaCluster);
	}

//	@Bean
//	public Map<String, Object> consumerConfigs() {
//		Map<String, Object> propsMap = new HashMap<>();
//		// list of host:port pairs used for establishing the initial connections to the Kakfa cluster
//		propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
//		propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaEnableAutoCommit);
//		propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, kafkaAutoCommitInterval);
//		propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaSessionTimeout);
//		propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaAutoOffsetReset);
//		return propsMap;
//	}
//
//	@Bean
//	public Map<String, Object> producerConfigs() {
//		Map<String, Object> props = new HashMap<>();
//		// list of host:port pairs used for establishing the initial connections to the Kakfa cluster
//		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
//		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//
//		return props;
//	}
//	
//	@Bean
//	public ProducerFactory<Integer, String> producerFactory() {
//		return new DefaultKafkaProducerFactory<>(producerConfigs());
//	}
//	
//	@Bean
//	public KafkaTemplate<Integer, String> kafkaTemplate() {
//		return new KafkaTemplate<Integer, String>(producerFactory());
//	}
}
