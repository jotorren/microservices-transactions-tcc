package net.jotorren.microservices.configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

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
}
