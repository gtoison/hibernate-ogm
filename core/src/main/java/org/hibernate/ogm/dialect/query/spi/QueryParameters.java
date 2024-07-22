/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * Represents the parameters passed to a query. Modelled after {@code QueryParameters} in Hibernate ORM, providing only
 * those values needed for OGM's purposes at this point.
 *
 * @author Gunnar Morling
 */
public class QueryParameters {

	private final RowSelection rowSelection;
	private final Map<String, TypedGridValue> namedParameters;
	private final List<TypedGridValue> positionalParameters;
	private final List<String> queryHints;

	public QueryParameters(RowSelection rowSelection, Map<String, TypedGridValue> namedParameters, List<TypedGridValue> positionalParameters, List<String> queryHints) {
		this.rowSelection = rowSelection;
		this.namedParameters = namedParameters;
		this.positionalParameters = positionalParameters;
		this.queryHints = queryHints;
	}
	
	public static QueryParameters fromJdbcParameterBindings(QueryParameterBindings queryBindings, QueryOptions queryOptions) {
		RowSelection rowSelection = new RowSelection( queryOptions.getFirstRow(), queryOptions.getMaxRows() );
		
		Map<String, TypedGridValue> namedParameters = new HashMap<String, TypedGridValue>();
		
		
		queryBindings.visitBindings( (parameter, binding) -> {
			namedParameters.put( parameter.getName(), new TypedGridValue( binding.getType(), binding.getBindValue() ) );
		});
		
		List<TypedGridValue> positionalParameters = Collections.emptyList();
		
		return new QueryParameters( rowSelection, namedParameters, positionalParameters, queryOptions.getDatabaseHints() );
	}

	public RowSelection getRowSelection() {
		return rowSelection;
	}

	public Map<String, TypedGridValue> getNamedParameters() {
		return namedParameters;
	}

	public List<TypedGridValue> getPositionalParameters() {
		return positionalParameters;
	}

	public List<String> getQueryHints() {
		return queryHints;
	}
}
