package net.jotorren.microservices.content.service;

import java.util.List;
import java.util.UUID;

import net.jotorren.microservices.content.dao.ContentDao;
import net.jotorren.microservices.content.dao.ContentTransactionAwareDao;
import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.context.ThreadLocalContext;
import net.jotorren.microservices.tx.CompositeTransactionDao;
import net.jotorren.microservices.tx.CompositeTransactionParticipantService;
import net.jotorren.microservices.tx.EntityCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ContentService extends CompositeTransactionParticipantService {

	private static final Logger LOG = LoggerFactory.getLogger(ContentService.class);
	
	@Autowired
    private ApplicationContext context;
	
	@Autowired
	private ContentDao dao;
	
	public String addNewContent(SourceCodeItem content) {
		String uuid = UUID.randomUUID().toString();
		content.setItemId(uuid);
		
		SourceCodeItem saved = dao.save(content);
		return saved.getItemId();
	}
	
	public SourceCodeItem getContent(String pk) {
		return dao.findOne(pk);
	}

	// Composite Transaction methods

	@Override
	public CompositeTransactionDao getCompositeTransactionDao() {
		return context.getBean(ContentTransactionAwareDao.class);
	}
	
	public String addNewContent(String txId, SourceCodeItem content) {
		ThreadLocalContext.put(CURRENT_TRANSACTION_KEY, txId);
		
		String uuid = UUID.randomUUID().toString();
		content.setItemId(uuid);
		
		LOG.info("Creating transaction [{}]", txId);
		SourceCodeItem saved = getCompositeTransactionDao().saveOrUpdate(content);
		
		return saved.getItemId();
	}
	
	public SourceCodeItem getContent(String txId, String pk) {
		ThreadLocalContext.remove(CURRENT_TRANSACTION_KEY);
		
		LOG.warn("Looking for transaction [{}]", txId);
		List<EntityCommand<?>> transactionOperations = getCompositeTransactionManager().fetch(txId);
		if (null == transactionOperations){
			LOG.error("Transaction [{}] does not exist", txId);
			return null;
		}
		ContentTransactionAwareDao unsynchronizedDao = (ContentTransactionAwareDao)getCompositeTransactionDao();
		unsynchronizedDao.apply(transactionOperations);
		
		return unsynchronizedDao.findOne(pk);
	}
}
