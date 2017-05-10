package net.jotorren.microservices.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class SpringContext {

	private static final Logger LOG = LoggerFactory.getLogger(SpringContext.class);

	private SpringContext() {
	}

	public static <T> T getBean(Class<T> clazz) {

		if (null == SpringContextProvider.getApplicationContext()) {
			LOG.warn("No application context available");
			return null;
		} else {
			return SpringContextProvider.getApplicationContext().getBean(clazz);
		}
	}
}