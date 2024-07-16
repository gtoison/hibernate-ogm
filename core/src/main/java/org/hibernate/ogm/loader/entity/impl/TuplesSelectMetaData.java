/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.sql.ast.spi.SqlSelection;

/**
 * 
 * @author Guillaume Toison
 */
public class TuplesSelectMetaData implements ResultSetMetaData {
	private List<SqlSelection> sqlSelections;
	
	public TuplesSelectMetaData(List<SqlSelection> sqlSelections) {
		this.sqlSelections = sqlSelections;
	}
	
	/**
	 * @see ResultSet#findColumn(String)
	 */
	public int findColumn(String columnLabel) {
		for (int i=0; i<sqlSelections.size(); i++) {
			SqlSelection selection = sqlSelections.get( i );
			String selectionColumnName = selection.getExpression().getColumnReference().getColumnExpression();
			if (selectionColumnName.equals( columnLabel )) {
				return i;
			}
			
		}
		
		return -1;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public int getColumnCount() throws SQLException {
		return sqlSelections.size();
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return true;
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		return true;
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		return false;
	}

	@Override
	public int isNullable(int column) throws SQLException {
		return ResultSetMetaData.columnNullableUnknown;
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		return true;
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		return 0;
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		return null;
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return sqlSelections.get( column-1 ).getExpression().getColumnReference().getColumnExpression();
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return null;
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return 0;
	}

	@Override
	public int getScale(int column) throws SQLException {
		return 0;
	}

	@Override
	public String getTableName(int column) throws SQLException {
		return null;
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		return null;
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		return sqlSelections.get( column-1 ).getExpression().getExpressionType().getSingleJdbcMapping().getJdbcType().getDdlTypeCode();
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		return sqlSelections.get( column-1 ).getExpression().getExpressionType().getSingleJdbcMapping().getJdbcType().getFriendlyName();
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		return false;
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		return sqlSelections.get( column-1 ).getExpression().getColumnReference().getColumnExpression();
	}
}
