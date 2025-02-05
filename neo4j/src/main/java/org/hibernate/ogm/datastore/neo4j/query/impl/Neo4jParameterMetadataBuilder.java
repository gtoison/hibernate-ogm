/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.impl;

import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.RecognizerBasedParameterMetadataBuilder;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;

/**
 * {@link ParameterMetadataBuilder} for native Neo4j queries. The implementation is based on a <a
 * href="http://parboiled.org">parboiled</a> grammar.
 *
 * @author Gunnar Morling
 */
public class Neo4jParameterMetadataBuilder extends RecognizerBasedParameterMetadataBuilder {

	public Neo4jParameterMetadataBuilder(ServiceRegistryImplementor serviceRegistry) {
		super( serviceRegistry );
	}

	@Override
	public void parseQueryParameters(String nativeQuery, ParameterRecognizer journaler) {
		QueryParser parser = Parboiled.createParser( QueryParser.class, journaler );
		new RecoveringParseRunner<ParameterRecognizer>( parser.Query() ).run( nativeQuery );
	}
}
