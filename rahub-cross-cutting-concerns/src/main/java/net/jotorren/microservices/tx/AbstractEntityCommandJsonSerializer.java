package net.jotorren.microservices.tx;

import org.springframework.core.serializer.support.SerializationFailedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractEntityCommandJsonSerializer<T> implements Serializer<EntityCommand<T>> {

	private ObjectMapper jacksonMapper = new ObjectMapper();
	
	public ObjectMapper getJacksonMapper(){
		return jacksonMapper;
	}
	
	@Override
	public byte[] write(EntityCommand<T> object) throws SerializationFailedException {
		return writeToString(object).getBytes();
	}

	@Override
	public String writeToString(EntityCommand<T> object) throws SerializationFailedException {
		try {
			return jacksonMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException("Error performing EntityCommand serialization", e);
		}
	}

	@Override
	public EntityCommand<T> read(byte[] bytes) throws SerializationFailedException {
		return readFromString(new String(bytes));
	}

	@Override
	public abstract EntityCommand<T> readFromString(String chars) throws SerializationFailedException;

}
