/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.util.Arrays;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * DO NOT UPDATE: copy from ORM that will be backported asap.
 *
 * Change Loader to UniqueEntityLoader
 * Need to introduce a new contract to encompass the old and new (in .plan) loadEntityBatch logic
 * - loaderToUse.doQueryAndInitializeNonLazyCollections( session, qp, false ); // old
 * - final List results = loaders[i].loadEntityBatch( session, smallBatch, persister().getIdentifierType(), optionalObject, persister().getEntityName(), id, persister(), lockOptions );
 * The former qp is essentially equivalent to the params of the latter.
 * Create a new loadEntityBatch API with an adaptor
 *
 * The base contract for loaders capable of performing batch-fetch loading of entities using multiple primary key
 * values in the SQL <tt>WHERE</tt> clause.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see BatchingEntityLoaderBuilder
 */
public abstract class BatchingEntityLoader<T> implements SingleIdEntityLoader<T> {
	private static final Logger log = Logger.getLogger( BatchingEntityLoader.class );

	private final EntityPersister persister;

	public BatchingEntityLoader(EntityPersister persister) {
		this.persister = persister;
	}

	public EntityPersister persister() {
		return persister;
	}
	
	@Override
	public T load(Object pkValue, Object entityInstance, LockOptions lockOptions, SharedSessionContractImplementor session) {
		return load( pkValue, entityInstance, lockOptions, session );
	}

	protected QueryParameters buildQueryParameters(
			Object id,
			Object[] ids,
			Object optionalObject,
			LockOptions lockOptions) {
		Type[] types = new Type[ids.length];
		Arrays.fill( types, persister().getIdentifierType() );

		QueryParameters qp = new QueryParameters();
		qp.setPositionalParameterTypes( types );
		qp.setPositionalParameterValues( ids );
		qp.setOptionalObject( optionalObject );
		qp.setOptionalEntityName( persister().getEntityName() );
		qp.setOptionalId( id );
		qp.setLockOptions( lockOptions );
		return qp;
	}

	protected Object getObjectFromList(List results, Object id, SharedSessionContractImplementor session) {
		for ( Object obj : results ) {
			final boolean equal = persister.getIdentifierType().isEqual(
					id,
					session.getContextEntityIdentifier( obj ),
					session.getFactory()
			);
			if ( equal ) {
				return obj;
			}
		}
		return null;
	}

	protected Object doBatchLoad(
			Object id,
			BatchableEntityLoader loaderToUse,
			SharedSessionContractImplementor session,
			Object[] ids,
			Object optionalObject,
			LockOptions lockOptions) {

		final List results = loaderToUse.loadEntityBatch(
				session,
				ids,
				persister.getIdentifierType(),
				optionalObject,
				persister.getEntityName(),
				id,
				persister,
				lockOptions );
		return getObjectFromList( results, id, session );
	}

}
