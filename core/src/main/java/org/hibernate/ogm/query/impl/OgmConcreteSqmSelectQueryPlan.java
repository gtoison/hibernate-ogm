/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.loader.entity.impl.OgmSingleIdLoadPlan;
import org.hibernate.ogm.loader.entity.impl.TuplesSelectPreparedStatement;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.util.impl.TransactionContextHelper;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.AppliedGraphs;
import org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * TODO ask the ORM team if {@link ConcreteSqmSelectQueryPlan} could be more customizable
 * 
 * @author Guillaume Toison
 */
public class OgmConcreteSqmSelectQueryPlan<R> extends ConcreteSqmSelectQueryPlan<R> {
	private SqmSelectStatement<?> sqm;
	private String hql;
	private DomainParameterXref domainParameterXref;
	private RowTransformer<R> rowTransformer;
	private QueryOptions queryOptions;
	private Class<R> resultType;
		
	public OgmConcreteSqmSelectQueryPlan(SqmSelectStatement<?> sqm, String hql, DomainParameterXref domainParameterXref,
			Class<R> resultType, TupleMetadata tupleMetadata, QueryOptions queryOptions) {
		super( sqm, hql, domainParameterXref, resultType, tupleMetadata, queryOptions );
		
		this.sqm = sqm;
		this.hql = hql;
		this.domainParameterXref = domainParameterXref;
		this.rowTransformer = determineRowTransformer( sqm, resultType, tupleMetadata, queryOptions );
		this.queryOptions = queryOptions;
		
		if (resultType == null) {
			this.resultType = (Class<R>) sqm.getSelection().getJavaType();
		} else {
			this.resultType = resultType;
		}
	}
	
	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		SharedSessionContractImplementor session = executionContext.getSession();
		SessionFactoryImplementor factory = session.getSessionFactory();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		
		if (gridDialect instanceof QueryableGridDialect<?>) {
			QueryableGridDialect<Serializable> queryableDialect = (QueryableGridDialect<Serializable>) gridDialect;
			QueryParserService queryParserService = serviceRegistry.getService( QueryParserService.class );
			OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
			
			final SqmTranslation<SelectStatement> sqmInterpretation =
					factory.getQueryEngine().getSqmTranslatorFactory()
							.createSelectTranslator(
									sqm,
									executionContext.getQueryOptions(),
									domainParameterXref,
									executionContext.getQueryParameterBindings(),
									executionContext.getSession().getLoadQueryInfluencers(),
									factory,
									true
							)
							.translate();
			
			final SqlAstTranslator<JdbcOperationQuerySelect> selectTranslator =
					factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
							.buildSelectTranslator( factory, sqmInterpretation.getSqlAst() );

			final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref
					= SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmInterpretation::getJdbcParamsBySqmParam );
			
			final FromClauseAccess tableGroupAccess = sqmInterpretation.getFromClauseAccess();
			
			final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
					executionContext.getQueryParameterBindings(),
					domainParameterXref,
					jdbcParamsXref,
					session.getFactory().getRuntimeMetamodels().getMappingMetamodel(),
					tableGroupAccess::findTableGroup,
					new SqmParameterMappingModelResolutionAccess() {
						@Override @SuppressWarnings("unchecked")
						public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
							return (MappingModelExpressible<T>) sqmInterpretation.getSqmParameterMappingModelTypeResolutions().get(parameter);
						}
					},
					session
			);
			
			final ListResultsConsumer.UniqueSemantic uniqueSemantic;
			if ( sqm.producesUniqueResults() && !AppliedGraphs.containsCollectionFetches( queryOptions ) ) {
				uniqueSemantic = ListResultsConsumer.UniqueSemantic.NONE;
			}
			else {
				uniqueSemantic = ListResultsConsumer.UniqueSemantic.ALLOW;
			}
			
			final JdbcOperationQuerySelect jdbcSelect = selectTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
			try {
				final SubselectFetch.RegistrationHandler subSelectFetchKeyHandler = SubselectFetch.createRegistrationHandler(
						session.getPersistenceContext().getBatchFetchQueue(),
						sqmInterpretation.getSqlAst(),
						JdbcParametersList.empty(),
						jdbcParameterBindings
				);
				session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames(), true );
				
				// Build the query
				// TODO this fails if the dialect doesn't provide a query service, we shouls degrade to Hibernate Search
				QueryParsingResult dialectQuery = queryParserService.parseQuery( factory, sqm, executionContext );
				EntityMetadataInformation entityMetaData = null;
				BackendQuery<Serializable> query = new BackendQuery<Serializable>( (Serializable) dialectQuery.getQueryObject(), entityMetaData );
				
				QueryParameters queryParameters = QueryParameters.fromJdbcParameterBindings( executionContext.getQueryParameterBindings(), queryOptions );
				
				// Query the tuples
				TupleTypeContextImpl tupleTypeContext = new TupleTypeContextImpl(
						Collections.emptyList(),
						Collections.emptySet(),
						Collections.emptyMap(),
						Collections.emptyMap(),
						optionsService.context().getEntityOptions( resultType ),
						null,
						null
				);
				TupleContext tupleContext = new TupleContextImpl( tupleTypeContext, TransactionContextHelper.transactionContext( session ) );
				
				List<Tuple> tuples = new ArrayList<Tuple>();
				try (ClosableIterator<Tuple> queryResult = queryableDialect.executeBackendQuery( query, queryParameters, tupleContext )) {
					queryResult.forEachRemaining( tuples::add );
				}
				
				List<SqlSelection> sqlSelections = OgmSingleIdLoadPlan.extractJdbcSelections( session, jdbcSelect );
				Function<String, PreparedStatement> statementCreator = sql -> new TuplesSelectPreparedStatement( tuples, sqlSelections );
				
				List<R> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().executeQuery(
						jdbcSelect,
						jdbcParameterBindings,
						listInterpreterExecutionContext( hql, executionContext, jdbcSelect, subSelectFetchKeyHandler ),
						rowTransformer,
						null,
						statementCreator,
						ListResultsConsumer.instance( uniqueSemantic )
				);
				
				for (int i = 0; i < results.size(); i++) {
					R entity = results.get( i );
					Tuple tuple = tuples.get( i );
					
					TuplePointer tuplePointer = OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
					tuplePointer.setTuple( tuple );
				}
				
				return results;
			}
			finally {
				domainParameterXref.clearExpansions();
			}
		}
		else {
			SearchSession searchSession = Search.session( (Session) session );
		
			return searchSession.search( resultType ).where( f -> f.matchAll() ).fetchAllHits();
		}
	}
}
