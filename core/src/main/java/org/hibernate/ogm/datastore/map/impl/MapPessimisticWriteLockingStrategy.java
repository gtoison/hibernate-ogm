/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.map.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class MapPessimisticWriteLockingStrategy implements LockingStrategy {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	protected final EntityPersister lockable;
	protected final LockMode lockMode;
	protected final ValuedModelPart identifierGridType;

	private volatile MapDatastoreProvider provider;

	public MapPessimisticWriteLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.identifierGridType = lockable.getIdentifierMapping();
	}
	
	@Override
	public void lock(Object id, Object version, Object object, int timeout, EventSource session)
			throws StaleObjectStateException, LockingStrategyException {
		MapDatastoreProvider dataStore = getProvider( session );
		EntityKey key = EntityKeyBuilder.fromData(
				( (OgmEntityPersister) lockable ).getRootEntityKeyMetadata(),
				identifierGridType,
				id,
				session );
		dataStore.writeLock( key, timeout );
		// FIXME check the version number as well and raise an optimistic lock exception if there is an issue JPA 2 spec: 3.4.4.2
		// (Comment by Emmanuel)
	}

	protected final MapDatastoreProvider getProvider(SharedSessionContractImplementor session) {
		if ( provider == null ) {
			DatastoreProvider service = session.getFactory().getServiceRegistry().getService( DatastoreProvider.class );
			if ( service instanceof MapDatastoreProvider ) {
				provider = (MapDatastoreProvider) service;
			}
			else {
				log.unexpectedDatastoreProvider( service.getClass(), MapDatastoreProvider.class );
			}
		}
		return provider;
	}

}
