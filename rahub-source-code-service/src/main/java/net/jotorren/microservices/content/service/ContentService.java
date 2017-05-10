package net.jotorren.microservices.content.service;

import java.util.List;
import java.util.UUID;

import net.jotorren.microservices.content.dao.ContentDao;
import net.jotorren.microservices.content.dao.TransactionAwareContentDao;
import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.context.ThreadLocalContext;
import net.jotorren.microservices.tx.CompositeTransactionManager;
import net.jotorren.microservices.tx.EntityCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

	private static final Logger LOG = LoggerFactory.getLogger(ContentService.class);
	
	@Autowired
    private ApplicationContext context;
	
	@Autowired
	private ContentDao dao;

	@Autowired
	private CompositeTransactionManager txManager;
	
	
	public String save(SourceCodeItem content) {
		String uuid = UUID.randomUUID().toString();
		content.setItemId(uuid);
		
		SourceCodeItem saved = dao.save(content);
		return saved.getItemId();
	}
	
	public SourceCodeItem get(String pk) {
		return dao.findOne(pk);
	}

	// Composite Transaction methods
	
	public String save(String txId, SourceCodeItem content) {
		ThreadLocalContext.put("current.opened.tx", txId);
		
		String uuid = UUID.randomUUID().toString();
		content.setItemId(uuid);
		
		LOG.info("Creating transaction [{}]", txId);
		TransactionAwareContentDao unsynchronizedDao = context.getBean(TransactionAwareContentDao.class);
		SourceCodeItem saved = unsynchronizedDao.saveOrUpdate(content);
		
		return saved.getItemId();
	}
	
	public SourceCodeItem get(String txId, String pk) {
		ThreadLocalContext.remove("current.opened.tx");
		
		LOG.warn("Looking for transaction [{}]", txId);
		List<EntityCommand<?>> transactionOperations = txManager.fetch(txId);
		if (null == transactionOperations){
			LOG.error("Transaction [{}] does not exist", txId);
			return null;
		}
		TransactionAwareContentDao unsynchronizedDao = context.getBean(TransactionAwareContentDao.class);
		unsynchronizedDao.apply(transactionOperations);
		
		return unsynchronizedDao.findOne(pk);
	}

	// TCC methods
	
	@Transactional(readOnly=false)
	public void commit(String txId) {
		ThreadLocalContext.remove("current.opened.tx");
		
		LOG.warn("Looking for transaction [{}]", txId);
		List<EntityCommand<?>> transactionOperations = txManager.fetch(txId);
		if (null == transactionOperations){
			LOG.warn("Transaction [{}] does not exist. Ignoring commit call", txId);
			return;
		}
		TransactionAwareContentDao unsynchronizedDao = context.getBean(TransactionAwareContentDao.class);
		unsynchronizedDao.apply(transactionOperations);
		
		LOG.warn("Committing transaction [{}]", txId);
		unsynchronizedDao.commit();
	}
	
	public void rollback(String txId) {
		ThreadLocalContext.remove("current.opened.tx");
		
		LOG.warn("Rolling back transaction [{}]", txId);
	}
}
