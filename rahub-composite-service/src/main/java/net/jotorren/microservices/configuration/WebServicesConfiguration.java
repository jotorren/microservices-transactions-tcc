package net.jotorren.microservices.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.jotorren.microservices.composite.controller.CompositeController;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomikos.icatch.tcc.rest.CoordinatorImp;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Configuration
public class WebServicesConfiguration {

    @Autowired
    private Bus bus;
    
    @Autowired
    private CoordinatorImp tccCoordinatorService;
    
    @Autowired
    private CompositeController compositeController;
    
    @Bean
    public Server rsServer() {
        JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setAddress("/");
        endpoint.setProvider(new JacksonJsonProvider());
        
        Map<Object, Object> mappings = new HashMap<Object, Object>();
        mappings.put("json", "application/json");
        endpoint.setExtensionMappings(mappings);
        
        endpoint.setFeatures(Arrays.asList(new LoggingFeature()));

        endpoint.setServiceBeans(Arrays.asList(tccCoordinatorService, compositeController));
        
        return endpoint.create();
    }
}
