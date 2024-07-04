/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A {@link QueryTranslator} which converts JP-QL queries into store-dependent native queries, e.g. Cypher queries for
 * Neo4j or {@code DBObject}-based queries for MongoDB.
 * <p>
 * Query conversion is done by invoking the dialect's query parser service. Results are loaded through OgmQueryLoader.
 * Depending on whether a store supports parameterized queries (Neo4j does, MongoDB doesn't) we either use one and the
 * same loader for a query executed several times with different parameter values or we create a new loader for each set
 * of parameter values.
 *
 * @author Gunnar Morling
 */
public class OgmQueryTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Statement statement;

	private final QueryParserService queryParser;

	/**
	 * The query loader in case the dialect supports parameterized queries; We can re-execute it then with different
	 * parameter values.
	 */
	private OgmQueryLoader loader;

	/**
	 * Needed to create query loaders. This won't be required anymore once {@link OgmQueryLoader} doesn't depend that
	 * much on {@link QueryLoader}.
	 */
	private SelectClause selectClause;

	/**
	 * When the query is only targeting one type, we register the EntityMetadataInformation for this type.
	 */
	private EntityMetadataInformation singleEntityMetadataInformation;

	/**
	 * Not all stores support parameterized queries. As a temporary measure, we therefore cache created queries per set
	 * of parameter values. At one point, this should be replaced by caching the AST after validation but before the
	 * actual Lucene query is created.
	 */
	private final ConcurrentMap<CacheKey, QueryParsingResult> queryCache;

	public OgmQueryTranslator(SessionFactoryImplementor sessionFactory, QueryParserService queryParser, Statement statement) {
		super( sessionFactory, statement );
		
		this.queryParser = queryParser;
		this.statement = statement;

		queryCache = new BoundedConcurrentHashMap<CacheKey, QueryParsingResult>(
				100,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);
	}

	private static class CacheKey {

		private final Map<String, TypedValue> parameters;
		private final int hashCode;

		public CacheKey(Map<String, TypedValue> parameters) {
			this.parameters = Collections.unmodifiableMap( parameters );
			this.hashCode = parameters.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			CacheKey other = (CacheKey) obj;
			if ( parameters == null ) {
				if ( other.parameters != null ) {
					return false;
				}
			}
			else if ( !parameters.equals( other.parameters ) ) {
				return false;
			}
			return true;
		}
	}
}
