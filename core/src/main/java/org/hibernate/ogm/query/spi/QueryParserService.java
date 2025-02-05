/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.Service;

/**
 * Based on JP-QL queries, implementations create native queries in a representation understood by the underlying
 * datastore.
 * <p>
 * There should be a single QueryParserService implementation registered, but we expect to support multiple types using
 * different or hybrid strategies.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
@Experimental("This contract is still under active development")
public interface QueryParserService extends Service {

	/**
	 * Whether this implementation supports parameterized queries or not. If so, a given query needs to be parsed only
	 * once and can then be executed repeatedly with different parameter values. If not, parameterized queries must be
	 * parsed again for each given set of parameter values.
	 *
	 * @return {@code true} if this implementation supports parameterized queries, {@code false} otherwise.
	 */
	boolean supportsParameters();

	/**
	 * Parses the given query. If it has parameters, they will be resolved in the resulting query by applying the given
	 * parameter values.
	 *
	 * @param sessionFactory the session factory
	 * @param queryString the query to parse
	 * @param namedParameters contains the parameters of the query and the corresponding values
	 * @return the parsed query
	 */
	// TODO: Should SF be injected during construction?
	QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters);

	/**
	 * Parses the given query. If it has parameters, the resulting query will have parameters as well.
	 *
	 * @param sessionFactory the session factory
	 * @param queryString the query to parse
	 * @return the parsed query
	 */
	QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString);
	
	QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory,
			SqmSelectStatement<?> sqm,
			DomainQueryExecutionContext executionContext);
}
