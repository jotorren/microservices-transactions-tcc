package net.jotorren.microservices.context;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadLocalContext {

	private static final Logger LOG = LoggerFactory.getLogger(ThreadLocalContext.class);

	/** Initial size of the map */
	private static final int HT_SIZE = 5;

	/** Thread local map variable to store any shared object */
	private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_HOLDER = new ThreadLocal<Map<String, Object>>() {

		protected java.util.Map<String, Object> initialValue() {
			return new HashMap<String, Object>(HT_SIZE);
		};
	};

	private ThreadLocalContext() {
	}

	public static void put(String key, Object payload) {
		if (null == key) {
			LOG.error("Unable to put a null key");
			return;
		}

		if (null == payload) {
			LOG.error("Unable to put a null value");
			return;
		}

		THREAD_LOCAL_HOLDER.get().put(key, payload);
	}

	public static Object get(String key) {
		if (null == key) {
			LOG.error("Unable to get a null key");
			return null;
		}

		return THREAD_LOCAL_HOLDER.get().get(key);
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(String key, Class<T> requiredType) {
		return (T) get(key);
	}

	public static Object remove(String key) {
		if (null == key) {
			LOG.error("Unable to remove a null key");
			return null;
		}

		return THREAD_LOCAL_HOLDER.get().remove(key);
	}

	public static void cleanup() {
		THREAD_LOCAL_HOLDER.get().clear();
		THREAD_LOCAL_HOLDER.remove();
	}

	public static Map<String, Object> exportContext() {
		return THREAD_LOCAL_HOLDER.get();
	}

	public static void importContext(Map<String, Object> context) {
		THREAD_LOCAL_HOLDER.get().putAll(context);
	}
}
