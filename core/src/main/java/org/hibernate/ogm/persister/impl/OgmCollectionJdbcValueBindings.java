/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.sql.model.TableMapping;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmCollectionJdbcValueBindings implements JdbcValueBindings {
	private JdbcValueBindings delegate;
	
	public OgmCollectionJdbcValueBindings(JdbcValueBindings delegate) {
		this.delegate = delegate;
	}

	@Override
	public BindingGroup getBindingGroup(String tableName) {
		return delegate.getBindingGroup( tableName );
	}

	@Override
	public void bindValue(Object value, String tableName, String columnName, ParameterUsage usage) {
		delegate.bindValue( value, tableName, columnName, usage );
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		delegate.beforeStatement( statementDetails );
	}

	@Override
	public void afterStatement(TableMapping mutatingTable) {
		delegate.afterStatement( mutatingTable );
	}
}
