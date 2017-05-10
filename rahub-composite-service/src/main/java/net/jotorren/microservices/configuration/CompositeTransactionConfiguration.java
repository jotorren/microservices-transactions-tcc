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
public class CompositeTransactionConfiguration {

	@Value("${tcc.rest.coordinator.base.url}")
	private String tccCoordinatorBaseUrl;

	
	@Value("${zookeeper.servers}")
	private String zooServers;

	@Value("${zookeeper.session.timeout}")
	private int zooSessionTimeout;
	
	@Value("${zookeeper.connection.timeout}")
	private int zooConnectionTimeout;

	
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
}
