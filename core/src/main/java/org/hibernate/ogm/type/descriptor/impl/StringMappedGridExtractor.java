/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Nicolas Helleringer
 */
public class StringMappedGridExtractor<J> implements GridValueExtractor<J> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final GridTypeDescriptor gridTypeDescriptor;
	private final JavaType<J> javaTypeDescriptor;

	public StringMappedGridExtractor( JavaType<J> javaTypeDescriptor, GridTypeDescriptor gridTypeDescriptor ) {
		this.gridTypeDescriptor = gridTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public J extract(Tuple resultset, String name) {
		@SuppressWarnings( "unchecked" )
		final String result = (String) resultset.get( name );
		if ( result == null ) {
			log.tracef( "found [null] as column [$s]", name );
			return null;
		}
		else {
			final J resultJ = javaTypeDescriptor.fromString( result );
			if ( log.isTraceEnabled() ) {
				log.tracef( "found [$s] as column [$s]", javaTypeDescriptor.extractLoggableRepresentation( resultJ ), name );
			}
			return resultJ;
		}
	}
}
