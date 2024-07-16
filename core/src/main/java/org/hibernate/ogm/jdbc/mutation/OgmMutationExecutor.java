/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jdbc.mutation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper;
import org.hibernate.engine.jdbc.mutation.internal.MutationExecutorStandard;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.TableMapping;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmMutationExecutor extends MutationExecutorStandard {

	public OgmMutationExecutor(
			MutationOperationGroup mutationOperationGroup,
			BatchKeyAccess batchKeySupplier,
			int batchSize,
			SharedSessionContractImplementor session) {
		super( mutationOperationGroup, batchKeySupplier, batchSize, session );
	}
	
	@Override
	protected void performNonBatchedMutation(
			PreparedStatementDetails statementDetails,
			Object id,
			JdbcValueBindings valueBindings,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
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

			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

			if ( affectedRowCount == 0 && tableDetails.isOptional() ) {
				// the optional table did not have a row
				return;
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
}
