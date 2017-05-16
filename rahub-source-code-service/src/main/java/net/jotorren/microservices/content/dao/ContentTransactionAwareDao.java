package net.jotorren.microservices.content.dao;

import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.tx.CompositeTransactionParticipantDao;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("prototype")
public class ContentTransactionAwareDao extends CompositeTransactionParticipantDao{

    public SourceCodeItem findOne(String pk){    	    	  
    	return getEntityManager().find(SourceCodeItem.class, pk);
    }
}
