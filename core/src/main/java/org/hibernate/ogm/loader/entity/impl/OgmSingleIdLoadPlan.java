/*
 * Hsibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Function;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.SingleIdLoadPlan;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.jdbc.impl.JdbcOgmMapper;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.TransactionContextHelper;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Guillaume Toison
 */
public class OgmSingleIdLoadPlan<T> extends SingleIdLoadPlan<T> {

	public OgmSingleIdLoadPlan(
			EntityMappingType entityMappingType,
			ModelPart restrictivePart,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		super( entityMappingType, restrictivePart, sqlAst, jdbcParameters, lockOptions, sessionFactory );
	}

	@Override
	public T load(Object restrictedValue, Object entityInstance, Boolean readOnly, Boolean singleResultExpected,
			SharedSessionContractImplementor session) {
		final EntityIdentifierMapping restrictivePart = (EntityIdentifierMapping) getRestrictivePart();
		final JdbcParametersList jdbcParameters = getJdbcParameters();
		final LockOptions lockOptions = getLockOptions();
		final JdbcOperationQuerySelect jdbcSelect = getJdbcSelect();
		final EntityMappingType entityMappingType = (EntityMappingType) getLoadable();
		final OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityMappingType.getEntityPersister();
		
		final int jdbcTypeCount = restrictivePart.getJdbcTypeCount();
		assert jdbcParameters.size() % jdbcTypeCount == 0;

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcTypeCount );

		int offset = 0;
		while ( offset < jdbcParameters.size() ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					restrictedValue,
					offset,
					restrictivePart,
					jdbcParameters,
					session
			);
		}
		assert offset == jdbcParameters.size();
		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, readOnly );
		final Callback callback = new CallbackImpl();
		
		ServiceRegistryImplementor serviceRegistry = session.getFactory().getServiceRegistry();
		GridDialect dialect = serviceRegistry.getService( GridDialect.class );
		
		EntityKey entityKey = JdbcOgmMapper.extractKey( restrictedValue, session, restrictivePart );
		TupleTypeContextImpl tupleTypeContext = ogmEntityPersister.createTupleTypeContext( serviceRegistry );
		OperationContext operationContext = new TupleContextImpl( tupleTypeContext, TransactionContextHelper.transactionContext( session ) );
		
		Tuple tuple = dialect.getTuple( entityKey, operationContext );
		
		List<SqlSelection> sqlSelections = extractJdbcSelections( session, jdbcSelect );
		
		Function<String, PreparedStatement> statementCreator = sql -> new TuplesSelectPreparedStatement( tuple, sqlSelections );
		ListResultsConsumer.UniqueSemantic uniqueSemantic = singleResultExpected ? ListResultsConsumer.UniqueSemantic.ASSERT : ListResultsConsumer.UniqueSemantic.FILTER;
		
		final List<T> list = session.getJdbcServices().getJdbcSelectExecutor().executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				new SingleIdExecutionContext(
						session,
						entityInstance,
						restrictedValue,
						entityMappingType.getRootEntityDescriptor(),
						queryOptions,
						callback
				),
				getRowTransformer(),
				null,
				statementCreator,
				ListResultsConsumer.instance( uniqueSemantic )
		);
		
		if ( list.isEmpty() ) {
			return null;
		}

		final T entity = list.get( 0 );
		
		callback.invokeAfterLoadActions( entity, entityMappingType, session );
		
		// Save the tuple in the state so we can retrieve it later if we updated the instance
		TuplePointer tuplePointer = OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
		tuplePointer.setTuple( tuple );
		
		return entity;
	}

	public static List<SqlSelection> extractJdbcSelections(SharedSessionContractImplementor session,
			final JdbcOperationQuerySelect jdbcSelect) {
		// TODO check what we should be doing here, 
		JdbcValuesMetadata jdbcResultsMetadata = new JdbcValuesMetadata() {
			
			@Override
			public <J> BasicType<J> resolveType(int position, JavaType<J> explicitJavaType,
					TypeConfiguration typeConfiguration) {
				return null;
			}
			
			@Override
			public int resolveColumnPosition(String columnName) {
				return 0;
			}
			
			@Override
			public String resolveColumnName(int position) {
				return null;
			}
			
			@Override
			public int getColumnCount() {
				return 0;
			}
		};
		
		LoadQueryInfluencers loadqueryInfluencers = session.getLoadQueryInfluencers();
		JdbcValuesMapping valuesMapping = jdbcSelect.getJdbcValuesMappingProducer().resolve( jdbcResultsMetadata, loadqueryInfluencers, session.getFactory() );
		List<SqlSelection> sqlSelections = valuesMapping.getSqlSelections();
		return sqlSelections;
	}


	/**
	 * Copied from {@link SingleIdLoadPlan}
	 * TODO check if we can make this visible
	 */
	private static class SingleIdExecutionContext extends BaseExecutionContext {
		private final Object entityInstance;
		private final Object restrictedValue;
		private final EntityMappingType rootEntityDescriptor;
		private final QueryOptions queryOptions;
		private final Callback callback;

		public SingleIdExecutionContext(
				SharedSessionContractImplementor session,
				Object entityInstance,
				Object restrictedValue,
				EntityMappingType rootEntityDescriptor, QueryOptions queryOptions,
				Callback callback) {
			super( session );
			this.entityInstance = entityInstance;
			this.restrictedValue = restrictedValue;
			this.rootEntityDescriptor = rootEntityDescriptor;
			this.queryOptions = queryOptions;
			this.callback = callback;
		}

		@Override
		public Object getEntityInstance() {
			return entityInstance;
		}

		@Override
		public Object getEntityId() {
			return restrictedValue;
		}

		@Override
		public EntityMappingType getRootEntityDescriptor() {
			return rootEntityDescriptor;
		}

		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Override
		public Callback getCallback() {
			return callback;
		}

	}
}
