package net.jotorren.microservices.tx;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import net.jotorren.microservices.context.SpringContext;
import net.jotorren.microservices.context.ThreadLocalContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeStateJpaListener {

	private static final Logger LOG = LoggerFactory.getLogger(ChangeStateJpaListener.class);

	private void enlist(Object entity, EntityCommand.Action action, String txId){
		
		EntityCommand<Object> command = new EntityCommand<Object>();
		command.setEntity(entity);
		command.setAction(action);
		command.setTransactionId(txId);
		command.setTimestamp(System.currentTimeMillis());
		
		CompositeTransactionManager txManager = SpringContext.getBean(CompositeTransactionManager.class);
		txManager.enlist(txId, command);
	}
	
	@PrePersist
	void onPrePersist(Object o) {
		String txId = (String)ThreadLocalContext.get("current.opened.tx");
		if (null == txId){
			LOG.info("onPrePersist outside any transaction");
		} else {
			LOG.info("onPrePersist inside transaction [{}]", txId);
			enlist(o, EntityCommand.Action.INSERT, txId);
		}
	}

	@PreUpdate
	void onPreUpdate(Object o) {
		String txId = (String)ThreadLocalContext.get("current.opened.tx");
		if (null == txId){
			LOG.info("onPreUpdate outside any transaction");
		} else {
			LOG.info("onPreUpdate inside transaction [{}]", txId);
			enlist(o, EntityCommand.Action.UPDATE, txId);
		}
	}

	@PreRemove
	void onPreRemove(Object o) {
		String txId = (String)ThreadLocalContext.get("current.opened.tx");
		if (null == txId){
			LOG.info("onPreRemove outside any transaction");
		} else {	
			LOG.info("onPreRemove inside transaction [{}]", txId);
			enlist(o, EntityCommand.Action.DELETE, txId);
		}
	}
}
