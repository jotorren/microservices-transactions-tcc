package net.jotorren.microservices.tx;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

public class CompositeTransactionDao {

	@PersistenceContext(type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
	private EntityManager em;

	public EntityManager getEntityManager() {
		return em;
	}
	
	public void save(Object entity) {
		em.persist(entity);
	}

	public <T> T saveOrUpdate(T entity) {
		return em.merge(entity);
	}

	public void remove(Object entity) {
		em.remove(entity);
	}

	public void commit() {
		em.joinTransaction();
	}

	public void apply(List<EntityCommand<?>> transactionOperations) {
		if (null == transactionOperations) {
			return;
		}

		for (EntityCommand<?> command : transactionOperations) {
			switch (command.getAction().ordinal()) {
			case 0:
				save(command.getEntity());
				break;
			case 1:
				saveOrUpdate(command.getEntity());
				break;
			case 2:
				remove(command.getEntity());
				break;
			}
		}
	}
}
