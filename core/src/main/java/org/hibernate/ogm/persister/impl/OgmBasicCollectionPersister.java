/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.loader.impl.OgmEmbeddedCollectionLoader;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.persister.collection.BasicCollectionPersister;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmBasicCollectionPersister extends BasicCollectionPersister implements OgmCollectionPersister {

	private final AssociationKeyMetadata associationKeyMetadata;
	
	private String mainSidePropertyName;
	private AssociationTypeContext associationTypeContext;
	
	public OgmBasicCollectionPersister(Collection collectionBinding, CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
		
		associationKeyMetadata = buildAssociationKeyMetadata( collectionBinding );
	}

	@Override
	public void postInstantiate() throws MappingException {
		super.postInstantiate();
		
		mainSidePropertyName = getUnqualifiedRole();
		associationTypeContext = getAssociationTypeContext( mainSidePropertyName );
	}
	
	@Override
	public AssociationKeyMetadata getAssociationKeyMetadata() {
		return associationKeyMetadata;
	}
	
	@Override
	public AssociationTypeContext getAssociationTypeContext() {
		return associationTypeContext;
	}

	@Override
	public String getMainSidePropertyName() {
		return mainSidePropertyName;
	}
	
	@Override
	public OgmEntityPersister getOwnerEntityPersister() {
		return (OgmEntityPersister) super.getOwnerEntityPersister();
	}
	
	@Override
	protected CollectionLoader createSingleKeyCollectionLoader(LoadQueryInfluencers loadQueryInfluencers) {
		return new OgmEmbeddedCollectionLoader( this, loadQueryInfluencers, getFactory() );
	}

	@Override
	public void remove(Object id, SharedSessionContractImplementor session) throws HibernateException {
		// "remove" refers to the collection, so we're clearing it
		Object owner = session.getPersistenceContext().getCollectionOwner( id, this );

		AssociationPersister associationPersister = getAssociationPersister( owner, id, session );
		Association association = associationPersister.getAssociationOrNull();

		if ( association != null ) {
			association.clear();

			associationPersister.flushToDatastore();
		}
	}
}
