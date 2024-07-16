/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jdbc.mutation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Collections;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.AbstractSingleMutationExecutor;
import org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.metamodel.mapping.TableDetails.KeyDetails;
import org.hibernate.ogm.compensation.impl.InvocationCollectingGridDialect;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.TransactionContextHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * 
 * @author Guillaume Toison
 */
/**
 * 
 * @author Guillaume Toison
 */
public class OgmMutationExecutorSingleNonBatched extends AbstractSingleMutationExecutor {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	
	private final PreparedStatementGroupSingleTable statementGroup;
	private final GeneratedValuesMutationDelegate generatedValuesDelegate;

	public OgmMutationExecutorSingleNonBatched(
			PreparableMutationOperation mutationOperation,
			GeneratedValuesMutationDelegate generatedValuesDelegate,
			SharedSessionContractImplementor session) {
		super( mutationOperation, session );
		this.generatedValuesDelegate = generatedValuesDelegate;
		this.statementGroup = new PreparedStatementGroupSingleTable( mutationOperation, generatedValuesDelegate, session );
		prepareForNonBatchedWork( null, session );
	}

	@Override
	protected PreparedStatementGroupSingleTable getStatementGroup() {
		return statementGroup;
	}

	@Override
	protected GeneratedValues performNonBatchedOperations(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		if ( generatedValuesDelegate != null ) {
			if (true ) {
				// TODO implement generated values
				// Not sure what's going to happen here...
				// Throw an exception so we don't forget to implement this
				throw new UnsupportedOperationException( "Generated values not implemented" );
			}
			
			return generatedValuesDelegate.performMutation(
					statementGroup.getSingleStatementDetails(),
					getJdbcValueBindings(),
					modelReference,
					session
			);
		}
		else {
			performNonBatchedMutation(
					statementGroup.getSingleStatementDetails(),
					null,
					getJdbcValueBindings(),
					inclusionChecker,
					resultChecker,
					session,
					modelReference
			);
			return null;
		}
	}
	
	/**
	 * @param modelReference The object we are mutating; we need it to get the state in case we are updatingg
	 */
	protected void performNonBatchedMutation(
			PreparedStatementDetails statementDetails,
			Object id,
			JdbcValueBindings valueBindings,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session,
			Object modelReference) {
		if ( statementDetails == null ) {
			return;
		}

		final TableMapping tableDetails = statementDetails.getMutatingTableDetails();
		if ( inclusionChecker != null && !inclusionChecker.include( tableDetails ) ) {
			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
				MODEL_MUTATION_LOGGER.tracef(
						"Skipping execution of secondary insert : %s",
						tableDetails.getTableName()
				);
			}
			return;
		}

		if ( id != null ) {
			assert !tableDetails.isIdentifierTable() : "Unsupported identifier table with generated id";
			( (EntityTableMapping) tableDetails ).getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> valueBindings.bindValue(
							jdbcValue,
							tableDetails.getTableName(),
							columnMapping.getColumnName(),
							ParameterUsage.SET
					),
					session
			);
		}

		// If we get here the statement is needed - make sure it is resolved
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

		try {
			valueBindings.beforeStatement( statementDetails );

			SessionFactoryImplementor factory = session.getSessionFactory();
			ServiceRegistry serviceRegistry = factory.getServiceRegistry();
			GridDialect dialect = serviceRegistry.getService( GridDialect.class );
			OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
			
			String tableName = statementDetails.getMutatingTableDetails().getTableName();
			String[] keyColumnNames = buildKeyColumnNames();
			EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( tableName, keyColumnNames );
			EntityKey entityKey = buildEntityKey( valueBindings, tableName, keyColumnNames, keyMetadata );
			
			TupleTypeContext tupleTypeContext = buildTupleTypeContext( optionsService );
			TupleContext operationContext = new TupleContextImpl( tupleTypeContext, TransactionContextHelper.transactionContext( session ) );
			MutationType mutationType = getMutationOperation().getMutationType();

			// Check for duplicate if needed
			if ( mutationType == MutationType.INSERT && dialect.getDuplicateInsertPreventionStrategy( keyMetadata ) == DuplicateInsertPreventionStrategy.LOOK_UP ) {
				Tuple existingTuple = dialect.getTuple( entityKey, operationContext );

				if ( existingTuple != null ) {
					if ( dialect instanceof InvocationCollectingGridDialect ) {
						InvocationCollectingGridDialect invocationCollectingGridDialect = (InvocationCollectingGridDialect) dialect;
						try {
							invocationCollectingGridDialect.onInsertOrUpdateTupleFailure( entityKey, existingTuple,
									new TupleAlreadyExistsException(
											entityKey ) );
						} catch (TupleAlreadyExistsException taee) {
							throw log.mustNotInsertSameEntityTwice( MessageHelper.infoString( (EntityPersister) getMutationOperation().getMutationTarget(), id, factory ),
									taee );
						}
					}
					else {
						throw log.mustNotInsertSameEntityTwice( MessageHelper.infoString( (EntityPersister) getMutationOperation().getMutationTarget(), id, factory ), null );
					}
				}
			}

			int affectedRowCount;
			
			if ( mutationType == MutationType.DELETE ) {
				dialect.removeTuple( entityKey, operationContext );
				
				// Assume we have deleted the tuple: the assumption seems to be that we don't need to check its existence
				affectedRowCount = 1;
			}
			else if ( mutationType == MutationType.UPDATE ) {
				TuplePointer tuplePointer = getSharedTuplePointer( entityKey, modelReference, session, dialect, operationContext );
				
				updateTuple( valueBindings, tableName, tuplePointer.getTuple() );
				
				dialect.insertOrUpdateTuple( entityKey, tuplePointer, operationContext );
				
				// Assume we have updated the tuple
				affectedRowCount = 1;
			}
			else if ( mutationType == MutationType.INSERT ) {
				// Insert the tuple
				Tuple tuple = dialect.createTuple( entityKey, operationContext );
				
				updateTuple( valueBindings, tableName, tuple );
				
				TuplePointer tuplePointer = saveSharedTuple( modelReference, tuple, session );
				
				dialect.insertOrUpdateTuple( entityKey, tuplePointer, operationContext );
				
				affectedRowCount = tuple == null ? 0 : 1;
			}
			else {
				throw new IllegalArgumentException( "Unexepected mutation type: " + mutationType );
			}
			
			ModelMutationHelper.checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					String.format(
							"Unable to execute mutation PreparedStatement against table `%s`",
							tableDetails.getTableName()
					),
					statementDetails.getSqlString()
			);
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			valueBindings.afterStatement( tableDetails );
		}
	}

	private void updateTuple(JdbcValueBindings valueBindings, String tableName, Tuple tuple) {
		for (Binding binding : valueBindings.getBindingGroup( tableName ).getBindings()) {
			tuple.put( binding.getColumnName(), binding.getValue() );
		}
	}

	private TupleTypeContext buildTupleTypeContext( OptionsService optionsService ) {
		// TODO this is most likely incorrect, need to figure out how that matters
		return new TupleTypeContextImpl(
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.emptyMap(),
				optionsService.context().getEntityOptions( Object.class ),
				null,
				null
		);
	}

	private TuplePointer getSharedTuplePointer(EntityKey key, Object entity, SharedSessionContractImplementor session, GridDialect dialect, TupleContext tupleContext) {
		if ( entity == null ) {
			return new TuplePointer( dialect.getTuple( key, tupleContext ) );
		}

		return OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
	}

	private TuplePointer saveSharedTuple(Object entity, Tuple tuple, SharedSessionContractImplementor session) {
		TuplePointer tuplePointer = OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
		tuplePointer.setTuple( tuple );
		
		return tuplePointer;
	}
	
	private String[] buildKeyColumnNames() {
		KeyDetails keyDetails = getMutationOperation().getTableDetails().getKeyDetails();
		String[] columnNames = new String[keyDetails.getColumnCount()];
		
		for (int i=0; i<columnNames.length; i++) {
			columnNames[i] = keyDetails.getKeyColumn( i ).getColumnName();
		}
		
		return columnNames;
	}

	private EntityKey buildEntityKey(
			JdbcValueBindings valueBindings,
			String tableName,
			String[] keyColumnNames,
			EntityKeyMetadata keyMetadata) {
		Object[] keyValues = new Object[keyColumnNames.length];
		for (Binding binding : valueBindings.getBindingGroup( tableName ).getBindings()) {
			for ( int i=0; i<keyColumnNames.length; i++) {
				if (keyColumnNames[i].equals( binding .getColumnName())) {
					keyValues[i] = binding.getValue();
				}
			}
		}

		return new EntityKey( keyMetadata, keyValues );
	}

	@Override
	public void release() {
	}
}
