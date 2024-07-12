/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Emmanuel Bernard
 */
public abstract class BasicGridBinder<X> implements GridValueBinder<X> {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone( "UTC" );

	private static final WrapperOptions DEFAULT_OPTIONS = new WrapperOptions() {

		@Override
		public boolean useStreamForLobBinding() {
			return false;
		}

		@Override
		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return UTC_TIMEZONE;
		}

		@Override
		public SharedSessionContractImplementor getSession() {
			return null;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return null;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return 0;
		}
	};

	private final JavaType<X> javaDescriptor;
	private final GridTypeDescriptor gridDescriptor;

	public BasicGridBinder(JavaType<X> javaDescriptor, GridTypeDescriptor gridDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.gridDescriptor = gridDescriptor;
	}

	@Override
	public void bind(Tuple resultset, X value, String[] names) {
		if ( value == null ) {
			for ( String name : names ) {
				log.tracef( "binding [null] to parameter [%1$s]", name );
				resultset.put( name, null );
			}
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "binding [%1$s] to parameter(s) %2$s", javaDescriptor.extractLoggableRepresentation( value ), Arrays.toString( names ) );
			}
			doBind( resultset, value, names, DEFAULT_OPTIONS );
		}
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 */
	protected abstract void doBind(Tuple resultset, X value, String[] names, WrapperOptions options);
}
