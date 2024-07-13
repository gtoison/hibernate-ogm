/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.Map;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Base class for {@link ParameterMetadataBuilder}s based on ORM's {@link ParameterRecognizerImpl} SPI.
 *
 * @author Gunnar Morling
 */
public abstract class RecognizerBasedParameterMetadataBuilder implements ParameterMetadataBuilder {

	private final ServiceRegistryImplementor serviceRegistry;
	
	public RecognizerBasedParameterMetadataBuilder(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public ParameterMetadataImpl buildParameterMetadata(String nativeQuery) {
		// At the moment we don't care about JDBC base count for positional parameters because
		// we don't have a dialect that supports them.
		// After a quick inspection it seems that only with Neo4j would be easy to make it work,
		// and even in that scenario I'm not sure it will matter.
		// I think it makes sense to keep track of the JPA compliance problems in separate issues.
		// In this case after solving OGM-1407
		final ParameterRecognizerImpl parameterRecognizer = new ParameterRecognizerImpl();

		serviceRegistry.requireService( NativeQueryInterpreter.class )
				.recognizeParameters( nativeQuery, parameterRecognizer );
		
		final Map<Integer, QueryParameterImplementor<?>> ordinalDescriptors = parameterRecognizer.getPositionalQueryParameters();
		final Map<String, QueryParameterImplementor<?>> namedDescriptors = parameterRecognizer.getNamedQueryParameters();

		return new ParameterMetadataImpl( ordinalDescriptors, namedDescriptors );
	}

	/**
	 * Parses the given native NoSQL query string, collecting the contained named parameters in the course of doing so.
	 *
	 * @param noSqlQuery the query to parse
	 * @param recognizer collects any named parameters contained in the given query
	 */
	protected abstract void parseQueryParameters(String noSqlQuery, ParameterRecognizer recognizer);
}
