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

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.AbstractSingleMutationExecutor;
import org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.metamodel.mapping.TableDetails.KeyDetails;
import org.hibernate.ogm.compensation.impl.InvocationCollectingGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.jdbc.impl.JdbcOgmMapper;
import org.hibernate.ogm.model.impl.RowKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.EntityAssociationUpdater;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
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

	private AssociationPersister associationPersister;

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

			if (getMutationOperation().getMutationTarget() instanceof EntityMutationTarget) {
				mutateEntity( statementDetails, id, valueBindings, resultChecker, session, modelReference );
			}
			else if (getMutationOperation().getMutationTarget() instanceof CollectionMutationTarget) {
				mutateCollection( statementDetails, id, valueBindings, resultChecker, session, modelReference );
			}
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

	private void mutateEntity(PreparedStatementDetails statementDetails, Object id, JdbcValueBindings valueBindings,
			OperationResultChecker resultChecker, SharedSessionContractImplementor session, Object modelReference)
			throws SQLException {
		SessionFactoryImplementor factory = session.getSessionFactory();
		ServiceRegistry serviceRegistry = factory.getServiceRegistry();
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) getMutationOperation().getMutationTarget();
		
		String tableName = statementDetails.getMutatingTableDetails().getTableName();
		String[] keyColumnNames = buildKeyColumnNames();
		EntityKeyMetadata keyMetadata = ogmEntityPersister.getEntityKeyMetadata();
		EntityKey entityKey = JdbcOgmMapper.buildEntityKey( valueBindings, tableName, keyColumnNames, keyMetadata, session );
		
		TupleContext operationContext = ogmEntityPersister.getTupleContext( session );
		MutationType mutationType = getMutationOperation().getMutationType();

		// Check for duplicate if needed
		if ( mutationType == MutationType.INSERT && gridDialect.getDuplicateInsertPreventionStrategy( keyMetadata ) == DuplicateInsertPreventionStrategy.LOOK_UP ) {
			Tuple existingTuple = gridDialect.getTuple( entityKey, operationContext );

			if ( existingTuple != null ) {
				if ( gridDialect instanceof InvocationCollectingGridDialect ) {
					InvocationCollectingGridDialect invocationCollectingGridDialect = (InvocationCollectingGridDialect) gridDialect;
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
			removeInverseAssociations( session, modelReference, gridDialect, ogmEntityPersister, entityKey, operationContext );

			gridDialect.removeTuple( entityKey, operationContext );
			
			// Assume we have deleted the tuple: the assumption seems to be that we don't need to check its existence
			affectedRowCount = 1;
		}
		else if ( mutationType == MutationType.UPDATE ) {
			TuplePointer tuplePointer = getSharedTuplePointer( entityKey, modelReference, session, gridDialect, operationContext );
			
			JdbcOgmMapper.updateTuple( valueBindings, tableName, tuplePointer.getTuple(), session );

			removeInverseAssociations( session, modelReference, gridDialect, ogmEntityPersister, entityKey, operationContext );
			
			gridDialect.insertOrUpdateTuple( entityKey, tuplePointer, operationContext );
			
			// Assume we have updated the tuple
			affectedRowCount = 1;
		}
		else if ( mutationType == MutationType.INSERT ) {
			// Insert the tuple
			Tuple tuple = gridDialect.createTuple( entityKey, operationContext );
			
			JdbcOgmMapper.updateTuple( valueBindings, tableName, tuple, session );
			
			TuplePointer tuplePointer = saveSharedTuple( modelReference, tuple, session );
			
			gridDialect.insertOrUpdateTuple( entityKey, tuplePointer, operationContext );
			
			id = ogmEntityPersister.getIdentifier( modelReference, session );
			ogmEntityPersister.addToInverseAssociations( tuple, 0, id, modelReference, session );
			
			affectedRowCount = tuple == null ? 0 : 1;
		}
		else {
			throw new IllegalArgumentException( "Unexepected mutation type: " + mutationType );
		}
		
		ModelMutationHelper.checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
	}

	private void removeInverseAssociations(SharedSessionContractImplementor session, Object modelReference, GridDialect gridDialect,
			OgmEntityPersister ogmEntityPersister, EntityKey entityKey, TupleContext operationContext) {
		boolean mightManageInverseAssociations = ogmEntityPersister.mightManageInverseAssociations();
		
		if ( gridDialect.usesNavigationalInformationForInverseSideOfAssociations() ) {
			Object id = ogmEntityPersister.getIdentifier( modelReference, session );
			
			//delete inverse association information
			//needs to be executed before the tuple removal because the AtomicMap in ISPN is cleared upon removal
			if ( mightManageInverseAssociations ) {
				Tuple currentState = gridDialect.getTuple( entityKey, operationContext );
				new EntityAssociationUpdater( ogmEntityPersister )
						.id( id )
						.entity( modelReference )
						.resultset( currentState )
						.session( session )
						.tableIndex( 0 )
						.propertyMightRequireInverseAssociationManagement( ogmEntityPersister.getPropertyMightBeMainSideOfBidirectionalAssociation() )
						.removeNavigationalInformationFromInverseSide();
			}

			if (ogmEntityPersister.mightHaveNavigationalInformation() ) {
				ogmEntityPersister.removeNavigationInformation( id, modelReference, session );
			}
		}
	}

	private void mutateCollection(PreparedStatementDetails statementDetails, Object id, JdbcValueBindings valueBindings,
			OperationResultChecker resultChecker, SharedSessionContractImplementor session, Object modelReference)
			throws SQLException {
		OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) getMutationOperation().getMutationTarget();
		String tableName = statementDetails.getMutatingTableDetails().getTableName();
		
		MutationType mutationType = getMutationOperation().getMutationType();
		
		// If we had access to the collection we could do collection.getOwner() here
		// Instead we do this ugly hack: getting the value from the first binding and loading the owner
		Object ownerId = valueBindings.getBindingGroup( tableName ).getBindings().iterator().next().getValue();
		String ownerEntityName = ogmCollectionPersister.getOwnerEntityPersister().getEntityName();
		Object owner = session.internalLoad( ownerEntityName, ownerId, false, false );
		associationPersister = ogmCollectionPersister.getAssociationPersister( owner, ownerId, session );
		
		Tuple associationRow = new Tuple();
		RowKeyBuilder rowKeyBuilder = ogmCollectionPersister.initializeRowKeyBuilder();
		JdbcOgmMapper.updateTuple( valueBindings, tableName, associationRow, session );
		RowKey rowKey = rowKeyBuilder.values( associationRow ).build();

		int affectedRowCount;
		
		if ( mutationType == MutationType.DELETE ) {
			associationPersister.getAssociation().remove( rowKey );
			
			affectedRowCount = 1;
		}
		else if ( mutationType == MutationType.UPDATE ) {
			throw new UnsupportedOperationException( "TODO implement this" );
			
//			affectedRowCount = 1;
		}
		else if ( mutationType == MutationType.INSERT ) {
			associationPersister.getAssociation().put( rowKey, associationRow );
			
			affectedRowCount = 1;
		}
		else {
			throw new IllegalArgumentException( "Unexepected mutation type: " + mutationType );
		}
		
		ModelMutationHelper.checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
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

	@Override
	public void release() {
		if ( associationPersister != null ) {
			associationPersister.flushToDatastore();
			associationPersister = null;
		}
	}
}
