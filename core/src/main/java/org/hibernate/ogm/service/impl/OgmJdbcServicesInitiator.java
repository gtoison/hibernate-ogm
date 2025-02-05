/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

/**
 * Return a JdbcServicesImpl that does not access the underlying database
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmJdbcServicesInitiator implements StandardServiceInitiator<JdbcServices> {
	public static final OgmJdbcServicesInitiator INSTANCE = new OgmJdbcServicesInitiator();

	@Override
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}

	@Override
	public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmJdbcServicesImpl();
	}

	private static final class OgmJdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
		public JdbcServicesImpl delegate = new JdbcServicesImpl();

		@Override
		public void configure(Map configurationValues) {
			configurationValues.put( "hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE );
			delegate.configure( configurationValues );
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			delegate.injectServices( serviceRegistry );
		}

		@Override
		public Dialect getDialect() {
			return delegate.getDialect();
		}

		@Override
		public SqlStatementLogger getSqlStatementLogger() {
			return delegate.getSqlStatementLogger();
		}

		@Override
		public SqlExceptionHelper getSqlExceptionHelper() {
			return delegate.getSqlExceptionHelper();
		}

		@Override
		public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
			return delegate.getExtractedMetaDataSupport();
		}

		@Override
		public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
			return delegate.getLobCreator( lobCreationContext );
		}

		@Override
		public JdbcEnvironment getJdbcEnvironment() {
			return delegate.getJdbcEnvironment();
		}

		@Override
		public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
			return delegate.getBootstrapJdbcConnectionAccess();
		}

		@Override
		public ParameterMarkerStrategy getParameterMarkerStrategy() {
			return delegate.getParameterMarkerStrategy();
		}
	}
}
