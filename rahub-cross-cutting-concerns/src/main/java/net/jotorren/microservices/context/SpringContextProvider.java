package net.jotorren.microservices.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

// To be defined as spring component inside target project
public class SpringContextProvider implements ApplicationContextAware {
	private static ApplicationContext ctx = null;

	public static ApplicationContext getApplicationContext() {
		return ctx;
	}

	public void setApplicationContext(ApplicationContext ctx) {
		SpringContextProvider.ctx = ctx;
	}
}
