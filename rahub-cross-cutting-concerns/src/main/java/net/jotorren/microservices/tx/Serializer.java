package net.jotorren.microservices.tx;

import org.springframework.core.serializer.support.SerializationFailedException;

public interface Serializer<T> {

	byte[] write(T object) throws SerializationFailedException;
	String writeToString(T object) throws SerializationFailedException;
	
	T read(byte[] bytes) throws SerializationFailedException;
	T readFromString(String chars) throws SerializationFailedException;
}
