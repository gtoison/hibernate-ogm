/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.internal.SingleIdLoadPlan;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmSingleIdEntityLoader<T> extends SingleIdEntityLoaderStandardImpl<T> {

	private OgmDatabaseSnapshotExecutor databaseSnapshotExecutor;

	public OgmSingleIdEntityLoader(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory, (lockOptions, influencers) -> createLoadPlan( entityDescriptor, lockOptions, influencers, sessionFactory ) );
	}

	private static <T> SingleIdLoadPlan<T> createLoadPlan(
			EntityMappingType loadable,
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				loadable,
				// null here means to select everything
				null,
				loadable.getIdentifierMapping(),
				null,
				1,
				queryInfluencers,
				lockOptions,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		return new OgmSingleIdLoadPlan<>(
				loadable,
				loadable.getIdentifierMapping(),
				sqlAst,
				jdbcParametersBuilder.build(),
				lockOptions,
				sessionFactory
		);
	}
	
	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		if ( databaseSnapshotExecutor == null ) {
			databaseSnapshotExecutor = new OgmDatabaseSnapshotExecutor( getLoadable(), sessionFactory );
		}

		return databaseSnapshotExecutor.loadDatabaseSnapshot( id, session );
	}
}
