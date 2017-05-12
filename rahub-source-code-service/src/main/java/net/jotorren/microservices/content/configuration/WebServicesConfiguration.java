package net.jotorren.microservices.content.configuration;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import javax.annotation.PostConstruct;

import net.jotorren.microservices.content.controller.ContentController;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServicesConfiguration extends ResourceConfig {

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
		// Register endpoints, providers, ...
		this.register(ContentController.class);
	}
	
	@PostConstruct
	public void init() {
		// Register components where DI is needed
		this.configureSwagger();
	}
}
