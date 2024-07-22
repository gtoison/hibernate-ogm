/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import org.hibernate.metamodel.mapping.MappingModelExpressible;

/**
 * Represents a value and its grid type.the selection criteria of a query. Modelled after {@code TypedValue} in
 * Hibernate ORM.
 *
 * @author Gunnar Morling
 */
public class TypedGridValue {

	private final MappingModelExpressible<?> type;
	private final Object value;

	public TypedGridValue(MappingModelExpressible<?> type, Object value) {
		this.type = type;
		this.value = value;
	}

	public MappingModelExpressible<?> getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}
