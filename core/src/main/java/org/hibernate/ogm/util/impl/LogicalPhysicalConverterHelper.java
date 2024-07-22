/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Helper methods to convert an object value into its column values
 *
 * @author Emmanuel Bernard
 */
public class LogicalPhysicalConverterHelper {
	public static Object[] getColumnValuesFromResultset(Tuple resultset, String[] propertyColumnNames) {
		Object[] columnValues;
		final int nbrOfColumns = propertyColumnNames.length;
		columnValues = new Object[nbrOfColumns];
		for ( int columnIndex = 0; columnIndex < nbrOfColumns; columnIndex++ ) {
			columnValues[columnIndex] = resultset.get( propertyColumnNames[columnIndex] );
		}
		return columnValues;
	}

	public static Object[] getColumnsValuesFromObjectValue(Object uniqueKey, ValuedModelPart gridUniqueKeyType, String[] propertyColumnNames,
			SharedSessionContractImplementor session) {
		// TODO handle multi column keys
		return new Object[] {gridUniqueKeyType.getJdbcMapping( 0 ).convertToRelationalValue( uniqueKey )};
	}
}
