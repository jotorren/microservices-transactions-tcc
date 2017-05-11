package net.jotorren.microservices.forum.configuration;

import net.jotorren.microservices.forum.controller.ForumController;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServicesConfiguration extends ResourceConfig {

	public WebServicesConfiguration() {
		register(ForumController.class);
	}
}
