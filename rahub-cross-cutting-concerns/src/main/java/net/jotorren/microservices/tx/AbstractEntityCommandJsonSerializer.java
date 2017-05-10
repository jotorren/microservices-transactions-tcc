package net.jotorren.microservices.tx;

import org.springframework.core.serializer.support.SerializationFailedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractEntityCommandJsonSerializer implements Serializer<EntityCommand> {

	protected ObjectMapper jacksonMapper = new ObjectMapper();
	
	@Override
	public byte[] write(EntityCommand object) throws SerializationFailedException {
		return writeToString(object).getBytes();
	}

	@Override
	public String writeToString(EntityCommand object) throws SerializationFailedException {
		try {
			return jacksonMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException("Error performing EntityCommand serialization", e);
		}
	}

	@Override
	public EntityCommand read(byte[] bytes) throws SerializationFailedException {
		return readFromString(new String(bytes));
	}

	@Override
	public abstract EntityCommand readFromString(String chars) throws SerializationFailedException;

}
