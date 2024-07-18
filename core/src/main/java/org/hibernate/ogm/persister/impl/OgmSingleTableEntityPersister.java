/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderProvidedQueryImpl;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.ogm.loader.entity.impl.OgmSingleIdEntityLoader;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.query.named.NamedQueryMemento;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmSingleTableEntityPersister extends SingleTableEntityPersister {
	private final PersistentClass persistentClass;

	public OgmSingleTableEntityPersister(
			PersistentClass persistentClass,
			EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
		
		this.persistentClass = persistentClass;
	}

	// TODO check if we can pull this in a base class for all persisters
	@Override
	protected SingleIdEntityLoader<?> buildSingleIdEntityLoader() {
		if ( hasNamedQueryLoader() ) {
			// We must resolve the named query on-demand through the boot model because it isn't initialized yet
			final NamedQueryMemento memento = null; // getNamedQueryMemento( null );
			return new SingleIdEntityLoaderProvidedQueryImpl<>( this, memento );
		}
		return buildSingleIdEntityLoader( new LoadQueryInfluencers( getFactory() ) );
	}

	// TODO check if we can make this visible in parent class
	private boolean hasNamedQueryLoader() {
		return persistentClass.getLoaderName() != null;
	}

	private SingleIdEntityLoader<?> buildSingleIdEntityLoader(LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers.effectivelyBatchLoadable( this ) ) {
			// TODO this needs to be adjusted
			final int batchSize = loadQueryInfluencers.effectiveBatchSize( this );
			return getFactory().getServiceRegistry()
					.requireService( BatchLoaderFactory.class )
					.createEntityBatchLoader( batchSize, this, getFactory() );
		}
		else {
			return new OgmSingleIdEntityLoader<>( this, getFactory() );
		}
	}
}
