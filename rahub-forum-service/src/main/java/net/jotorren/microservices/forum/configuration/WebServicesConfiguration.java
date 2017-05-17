package net.jotorren.microservices.forum.configuration;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import net.jotorren.microservices.forum.controller.ForumController;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServicesConfiguration extends ResourceConfig {

    /**
     * maximum number of entity bytes to be logged (and buffered) - if the entity is larger,
     * logging filter will print (and buffer in memory) only the specified number of bytes
     * and print "...more..." string at the end. Negative values are interpreted as zero.
     */                     
	@Value("${spring.jersey.log.entity.size:2048}")
	private int maxlog;
	
	@Value("${swagger.title}")
	private String title;

	@Value("${swagger.description}")
	private String description;

	@Value("${swagger.version}")
	private String version;

	@Value("${swagger.contact}")
	private String contact;

	@Value("${swagger.schemes}")
	private String schemes;

	@Value("${swagger.basePath}")
	private String basePath;

	@Value("${swagger.resourcePackage}")
	private String resourcePackage;

	@Value("${swagger.prettyPrint}")
	private boolean prettyPrint;
	
	@Value("${swagger.scan}")
	private boolean scan;

	private void configureSwagger() {
		// Available at localhost:port/swagger.json
		this.register(ApiListingResource.class);
		this.register(SwaggerSerializers.class);

		BeanConfig config = new BeanConfig();
		// config.setConfigId(title);
		config.setTitle(title);
		config.setDescription(description);
		config.setVersion(version);
		config.setContact(contact);
		config.setSchemes(schemes.split(","));
		config.setBasePath(basePath);
		config.setResourcePackage(resourcePackage);
		config.setPrettyPrint(prettyPrint);
		config.setScan(scan);	
	}
	
	public WebServicesConfiguration() {
		register(ForumController.class);
		register(new LoggingFeature(
				Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), 
				Level.SEVERE, 
				LoggingFeature.Verbosity.PAYLOAD_ANY, 
				maxlog));
	}
	
	@PostConstruct
	public void init() {
		// Register components where DI is needed
		this.configureSwagger();
	}
}
