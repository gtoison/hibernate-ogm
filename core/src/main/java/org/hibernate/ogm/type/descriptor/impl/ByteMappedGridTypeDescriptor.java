/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A {@link GridTypeDescriptor} which stores/retrieves values from the grid unwrapping/wrapping them as {@link Byte}, delegating to a
 * given {@link JavaTypeDescriptor}.
 *
 * @author Davide D'Alto
 */
public class ByteMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final ByteMappedGridTypeDescriptor INSTANCE = new ByteMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap( value, Byte.class, options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, true );
	}
}
