/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi.sqm;

import java.lang.invoke.MethodHandles;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * @author Guillaume Toison
 */
public class OgmSqlAstTranslatorFactory implements SqlAstTranslatorFactory {
	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );
	
	@Override
	public SqlAstTranslator<JdbcOperationQuerySelect> buildSelectTranslator(SessionFactoryImplementor sessionFactory,
			SelectStatement statement) {
		return buildTranslator( sessionFactory, statement );
	}

	@Override
	public SqlAstTranslator<? extends JdbcOperationQueryMutation> buildMutationTranslator(
			SessionFactoryImplementor sessionFactory, MutationStatement statement) {
		return buildTranslator( sessionFactory, statement );
	}

	@Override
	public <O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(TableMutation<O> mutation,
			SessionFactoryImplementor sessionFactory) {
		return buildTranslator( sessionFactory, mutation );
	}

	private <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory,
			Statement statement) {
		QueryParserService queryParser = sessionFactory.getServiceRegistry().getService( QueryParserService.class );
		if ( queryParser != null ) {
			return queryParser.buildTranslator( sessionFactory, statement );
		}
		else {
			try {
				return new FullTextSearchQueryTranslator( sessionFactory, queryIdentifier, queryString, filters );
			}
			catch (Exception e) {
				throw LOG.cannotLoadLuceneParserBackend( e );
			}
		}
		
	}
}
