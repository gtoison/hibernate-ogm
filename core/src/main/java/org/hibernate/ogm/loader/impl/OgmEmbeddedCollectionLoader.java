/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.internal.CollectionLoaderSingleKey;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.loader.entity.impl.OgmSingleIdLoadPlan;
import org.hibernate.ogm.loader.entity.impl.TuplesSelectPreparedStatement;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmBasicCollectionPersister;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * Large portions of the code is from {@link CollectionLoaderSingleKey}
 * 
 * @author Guillaume Toison
 */
public class OgmEmbeddedCollectionLoader implements CollectionLoader {

	private final OgmBasicCollectionPersister collectionPersister;

	private final PluralAttributeMapping attributeMapping;

	private final int keyJdbcCount;

	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	public OgmEmbeddedCollectionLoader(
			OgmBasicCollectionPersister collectionPersister,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.collectionPersister = collectionPersister;
		this.attributeMapping = collectionPersister.getAttributeMapping();

		this.keyJdbcCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();
		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();

		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				1,
				influencers,
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);

		final QuerySpec querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final FromClause fromClause = querySpec.getFromClause();
		final TableGroup tableGroup = fromClause.getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		this.jdbcParameters = jdbcParametersBuilder.build();
		this.jdbcSelect = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return collectionPersister.getAttributeMapping();
	}

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		GridDialect gridDialect = session.getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		String ownerEntityName = collectionPersister.getOwnerEntityPersister().getEntityName();
		Object owner = session.internalLoad( ownerEntityName, key, false, false );
		Class<?> mappedClass = collectionPersister.getOwnerEntityPersister().getMappedClass();

		// Load the tuples corresponding to the collection elements
		AssociationPersister associationPersister = new AssociationPersister.Builder(mappedClass)
			.gridDialect( gridDialect )
			.key( key, attributeMapping.getKeyDescriptor() )
			.associationKeyMetadata( collectionPersister.getAssociationKeyMetadata() )
			.associationTypeContext( collectionPersister.getAssociationTypeContext() )
			.hostingEntity( owner )
			.session( session )
			.build();

		Association association = associationPersister.getAssociationOrNull();
		List<Tuple> tuples = new ArrayList<>();
		
		if ( association != null ) {
			for ( RowKey rowKey : association.getKeys() ) {
				tuples.add( association.get( rowKey ) );
			}
		}
		
		// Now execute the jdbc query
		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), key );

		final JdbcServices jdbcServices = session.getFactory().getJdbcServices();

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				jdbcParameters,
				jdbcParameterBindings
		);
		
		List<SqlSelection> sqlSelections = OgmSingleIdLoadPlan.extractJdbcSelections( session, jdbcSelect );
		Function<String, PreparedStatement> statementCreator = sql -> new TuplesSelectPreparedStatement( tuples, sqlSelections );

		jdbcServices.getJdbcSelectExecutor().executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				new CollectionLoaderSingleKeyExecutionContext( session, collectionKey, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				null,
				statementCreator,
				ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER )
		);

		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private static class CollectionLoaderSingleKeyExecutionContext extends BaseExecutionContext {
		private final CollectionKey collectionKey;
		private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

		CollectionLoaderSingleKeyExecutionContext(
				SharedSessionContractImplementor session,
				CollectionKey collectionKey,
				SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
			super( session );
			this.collectionKey = collectionKey;
			this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		}

		@Override
		public CollectionKey getCollectionKey() {
			return collectionKey;
		}

		@Override
		public void registerLoadingEntityHolder(EntityHolder holder) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}
}
