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
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.loader.entity.impl.OgmSingleIdLoadPlan;
import org.hibernate.ogm.loader.entity.impl.TuplesSelectPreparedStatement;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.persister.impl.OgmBasicCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
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
import org.hibernate.type.BasicType;

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
		final GridDialect gridDialect = session.getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		final String ownerEntityName = collectionPersister.getOwnerEntityPersister().getEntityName();
		final Object owner = session.internalLoad( ownerEntityName, key, false, false );
		final Class<?> mappedClass = collectionPersister.getOwnerEntityPersister().getMappedClass();

		// Load the tuples corresponding to the collection elements
		final AssociationPersister associationPersister = new AssociationPersister.Builder(mappedClass)
			.gridDialect( gridDialect )
			.key( key, attributeMapping.getKeyDescriptor() )
			.associationKeyMetadata( collectionPersister.getAssociationKeyMetadata() )
			.associationTypeContext( collectionPersister.getAssociationTypeContext() )
			.hostingEntity( owner )
			.session( session )
			.build();

		final boolean entityElements = collectionPersister.getElementType().isEntityType();
		final Association association = associationPersister.getAssociationOrNull();
		final List<Tuple> tuples = new ArrayList<>();
		
		String elementEntityName = null;
		OgmEntityPersister elementPersister = null;
		List<EntityKeyAndTuple> elementsKeysAndTuples = null;
		JdbcMapping elementIdentifierMapping = null;
		
		if ( association != null ) {
			if (entityElements) {
				// Entity elements: the tuple only contains the ID of the elements
				elementEntityName = collectionPersister.getElementType().getName();
				elementPersister = (OgmEntityPersister) session.getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( elementEntityName )
				.getEntityPersister();
				EntityKeyMetadata elementKeyMetadata = elementPersister.getEntityKeyMetadata();
				elementIdentifierMapping = ((BasicType<?>) elementPersister.getIdentifierType()).getJdbcMapping();
				
				TupleContext tupleContext = elementPersister.getTupleContext( session );
				
				List<Tuple> elementTuples;
				elementsKeysAndTuples = new ArrayList<>();
				
				if (gridDialect instanceof MultigetGridDialect) {
					MultigetGridDialect multigetGridDialect = (MultigetGridDialect) gridDialect;
					List<EntityKey> elementKeys = new ArrayList<>();
					
					for ( RowKey rowKey : association.getKeys() ) {
						// TODO handle multi valued keys
						Object value = rowKey.getColumnValues()[1];
						EntityKey elementKey = new EntityKey( elementKeyMetadata, new Object[] { value } );

						elementKeys.add( elementKey );
					}
					
					// TODO account for the case when the some tuples were not found, and @NotFound(IGNORE)
					elementTuples = multigetGridDialect.getTuples( elementKeys.toArray( EntityKey[]::new ), tupleContext );
					
					for (int i = 0; i < elementKeys.size(); i++) {
						elementsKeysAndTuples.add( new EntityKeyAndTuple( elementKeys.get( i ), elementTuples.get( i ) ) );
					}
				}
				else {
					elementTuples = new ArrayList<>();
					
					// This is effectively a N+1 case: we're doing one get per element, maybe we could get away with returning proxies somehow
					for ( RowKey rowKey : association.getKeys() ) {
						// TODO handle multi valued keys
						Object value = rowKey.getColumnValues()[1];

						EntityKey elementKey = new EntityKey( elementKeyMetadata, new Object[] { value } );
						Tuple tuple = gridDialect.getTuple( elementKey, tupleContext );

						// TODO account for the case when the tuple wasn't found, and @NotFound(IGNORE)
						elementTuples.add( tuple );
						elementsKeysAndTuples.add( new EntityKeyAndTuple( elementKey, tuple ) );
					}
				}
				
				String keyColumnName = association.getKeys().iterator().next().getColumnNames()[0];
				for (Tuple tuple : elementTuples) {
					// Add a value for the owner id because our select statement contains a column for it and the row reader expects a value
					Tuple tupleWithForeignKey = new Tuple( tuple.getSnapshot(), SnapshotType.UPDATE );
					tupleWithForeignKey.put( keyColumnName, key );
					
					tuples.add( tupleWithForeignKey );
				}
			}
			else {
				// Embeddable elements, the tuple already contains the data
				for ( RowKey rowKey : association.getKeys() ) {
					tuples.add( association.get( rowKey ) );
				}
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
		
		if (elementsKeysAndTuples != null) {
			// We cannot iterate over the collection because we are still initialising it
			for (EntityKeyAndTuple entry : elementsKeysAndTuples) {
				Object entity = session.internalLoad( elementEntityName, elementIdentifierMapping.convertToDomainValue( entry.entityKey.getColumnValues()[0] ), false, false );
				OgmEntityEntryState entityState = OgmEntityEntryState.getStateFor( session, entity );
				entityState.getTuplePointer().setTuple( entry.tuple );
			}
		}
		
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
	
	private static class EntityKeyAndTuple {
		private final EntityKey entityKey;
		private final Tuple tuple;
		
		public EntityKeyAndTuple(EntityKey entityKey, Tuple tuple) {
			this.entityKey = entityKey;
			this.tuple = tuple;
		}
	}
}
