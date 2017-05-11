package net.jotorren.microservices.forum.domain;

import java.io.IOException;

import net.jotorren.microservices.tx.AbstractEntityCommandJsonSerializer;
import net.jotorren.microservices.tx.EntityCommand;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class ForumSerializer extends AbstractEntityCommandJsonSerializer<Forum>{

	@Override
	public EntityCommand<Forum> readFromString(String chars)
			throws SerializationFailedException {
		try {
			EntityCommand<Forum> command = new EntityCommand<Forum>();
			
			JsonNode node = getJacksonMapper().readValue(chars, JsonNode.class);
		    command.setAction(EntityCommand.Action.valueOf(node.get("action").asText()));
		    command.setTransactionId(node.get("transactionId").asText());
		    command.setTimestamp(node.get("timestamp").asLong());			    
		    command.setEntity(getJacksonMapper().readValue(node.get("entity").toString(), Forum.class));
		    
		    return command;
		} catch (JsonParseException e) {
			throw new SerializationFailedException(e.getMessage());
		} catch (JsonMappingException e) {
			throw new SerializationFailedException(e.getMessage());
		} catch (IOException e) {
			throw new SerializationFailedException(e.getMessage());
		}
	}
}
