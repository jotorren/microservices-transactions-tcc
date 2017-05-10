package net.jotorren.microservices.content.configuration;

import net.jotorren.microservices.content.controller.ContentController;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServicesConfiguration extends ResourceConfig {

	public WebServicesConfiguration() {
		register(ContentController.class);
	}
}
