package net.jotorren.microservices.forum.dao;

import net.jotorren.microservices.forum.domain.Forum;
import net.jotorren.microservices.tx.CompositeTransactionParticipantDao;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("prototype")
public class ForumTransactionAwareDao extends CompositeTransactionParticipantDao{

    public Forum findOne(String pk){    	    	  
    	return getEntityManager().find(Forum.class, pk);
    }
}
