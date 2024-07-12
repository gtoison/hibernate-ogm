/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.ogm.datastore.mongodb.type.GridFS;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * A {@link org.hibernate.type.Type} representing a value stored as GridFS.
 *
 * @author Davide D'Alto
 */
public class GridFSType extends AbstractSingleColumnStandardBasicType<GridFS> {

	public static final GridFSType INSTANCE = new GridFSType();

	public GridFSType() {
		super( SqlDescriptor.INSTANCE, JavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "gridfs";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[]{ "gridfs", GridFS.class.getName() };
	}

	private static class SqlDescriptor implements JdbcType {

		public static final SqlDescriptor INSTANCE = new SqlDescriptor();

		@Override
		public int getJdbcTypeCode() {
			return 0;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
			throw new UnsupportedOperationException( "This is only supposed to be used by Hibernate OGM" );
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
			throw new UnsupportedOperationException( "This is only supposed to be used by Hibernate OGM" );
		}
	}

	private static class JavaDescriptor implements BasicJavaType<GridFS> {

		public static final JavaDescriptor INSTANCE = new JavaDescriptor();

		@Override
		public Class<GridFS> getJavaTypeClass() {
			return GridFS.class;
		}

		@Override
		public GridFS fromString(CharSequence string) {
			return string == null ? null : new GridFS( string.toString().getBytes() );
		}

		@Override
		public <X> X unwrap(GridFS value, Class<X> type, WrapperOptions options) {
			throw new UnsupportedOperationException( "This is only supposed to be used by Hibernate OGM" );
		}

		@Override
		public <X> GridFS wrap(X value, WrapperOptions options) {
			throw new UnsupportedOperationException( "This is only supposed to be used by Hibernate OGM" );
		}
	}
}
