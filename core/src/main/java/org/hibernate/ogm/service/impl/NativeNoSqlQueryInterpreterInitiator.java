/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * Contributes the {@link NativeNoSqlQueryInterpreter}. No service implementation will be registered in case the current
 * grid dialect does not support the execution of native queries.
 *
 * @author Gunnar Morling
 */
public class NativeNoSqlQueryInterpreterInitiator implements SessionFactoryServiceInitiator<NativeQueryInterpreter> {

	public static NativeNoSqlQueryInterpreterInitiator INSTANCE = new NativeNoSqlQueryInterpreterInitiator();

	private NativeNoSqlQueryInterpreterInitiator() {
	}
	
	@Override
	public NativeQueryInterpreter initiateService( SessionFactoryServiceInitiatorContext context ) {
		return getParameterMetadataRecognizer( context.getServiceRegistry() );
	}

	@Override
	public Class<NativeQueryInterpreter> getServiceInitiated() {
		return NativeQueryInterpreter.class;
	}

	private NativeQueryInterpreter getParameterMetadataRecognizer(ServiceRegistryImplementor registry) {
		QueryableGridDialect<?> queryableGridDialect = registry.getService( QueryableGridDialect.class );

		if ( queryableGridDialect != null ) {
			// TODO reimplement this
			return null;// new NativeNoSqlQueryInterpreter( queryableGridDialect );
		}
		else {
			return null;
		}
	}
}
