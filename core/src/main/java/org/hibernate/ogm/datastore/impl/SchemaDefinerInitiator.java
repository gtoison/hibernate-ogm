/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * Contributes the {@link SchemaDefiner} service as obtained via
 * {@link DatastoreProvider#getSchemaDefinerType()}.
 *
 * @author Gunnar Morling
 */
public class SchemaDefinerInitiator implements SessionFactoryServiceInitiator<SchemaDefiner> {

	public static final SchemaDefinerInitiator INSTANCE = new SchemaDefinerInitiator();

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	public Class<SchemaDefiner> getServiceInitiated() {
		return SchemaDefiner.class;
	}
	
	@Override
	public SchemaDefiner initiateService(SessionFactoryServiceInitiatorContext context) {
		DatastoreProvider datastoreProvider = context.getServiceRegistry().getService( DatastoreProvider.class );
		Class<? extends SchemaDefiner> schemaInitializerType = datastoreProvider.getSchemaDefinerType();

		if ( schemaInitializerType != null ) {
			try {
				return schemaInitializerType.newInstance();
			}
			catch (Exception e) {
				throw log.unableToInstantiateType( schemaInitializerType, e );
			}
		}

		return new BaseSchemaDefiner();
	}
}
