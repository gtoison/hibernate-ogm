/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.sql.InFragment;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * The discriminator is a column containing a different value for each entity type.
 *
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
class ColumnBasedDiscriminator implements EntityDiscriminator {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final Object NULL_DISCRIMINATOR = new MarkerObject( "<null discriminator>" );
	private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject( "<not null discriminator>" );

	private final String alias;
	private final String columnName;
	private final BasicType<?> discriminatorType;
	private final boolean forced;
	private final boolean needed;
	private final String sqlValue;
	private final Object value;
	private final Map<Object, String> subclassesByValue;

	public ColumnBasedDiscriminator(final PersistentClass persistentClass, final SessionFactoryImplementor factory, final Column column) {
		Dialect dialect = factory.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		forced = persistentClass.isForceDiscriminator();
		columnName = column.getQuotedName( dialect );
		alias = column.getAlias( dialect, persistentClass.getRootTable() );
		discriminatorType = getDiscriminatorType( persistentClass );
		value = value( persistentClass, discriminatorType );
		sqlValue = sqlValue( persistentClass, dialect, value, discriminatorType );
		subclassesByValue = subclassesByValue( persistentClass, value, discriminatorType );
		needed = true;
	}

	private static Map<Object, String> subclassesByValue(final PersistentClass persistentClass, Object value, Type type) {
		Map<Object, String> subclassesByDsicriminator = new HashMap<Object, String>();
		subclassesByDsicriminator.put( value, persistentClass.getEntityName() );

		if ( persistentClass.isPolymorphic() ) {
			for ( Subclass sc : persistentClass.getSubclasses() ) {
				subclassesByDsicriminator.put( value( sc, type ), sc.getEntityName() );
			}
		}
		return subclassesByDsicriminator;
	}

	public static String sqlValue(PersistentClass persistentClass, Dialect dialect,
			Object value, Type discriminatorType) {
		try {
			return obtainSqlValue( persistentClass, dialect, value, discriminatorType );
		}
		catch ( ClassCastException cce ) {
			throw log.illegalDiscrimantorType( discriminatorType.getName() );
		}
		catch ( Exception e ) {
			throw log.unableToConvertStringToDiscriminator( e );
		}
	}

	private static String obtainSqlValue(PersistentClass persistentClass, Dialect dialect,
			Object value, Type discriminatorType) throws Exception {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return InFragment.NULL;
		}

		if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return InFragment.NOT_NULL;
		}

		@SuppressWarnings("unchecked")
		DiscriminatorType<Object> dtype = (DiscriminatorType<Object>) discriminatorType;
		return dtype.getJavaTypeDescriptor().toString( value );
	}

	public static Object value(PersistentClass persistentClass, Type discriminatorType) {
		try {
			return obtainValue( persistentClass, discriminatorType );
		}
		catch ( ClassCastException cce ) {
			throw log.illegalDiscrimantorType( discriminatorType.getName() );
		}
		catch ( Exception e ) {
			throw log.unableToConvertStringToDiscriminator( e );
		}
	}

	private static Object obtainValue(PersistentClass persistentClass, Type discriminatorType)
			throws Exception {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return NULL_DISCRIMINATOR;
		}

		if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return NOT_NULL_DISCRIMINATOR;
		}

		@SuppressWarnings("unchecked")
		DiscriminatorType<Object> dtype = (DiscriminatorType<Object>) discriminatorType;
		return dtype.getJavaTypeDescriptor().fromString( persistentClass.getDiscriminatorValue() );
	}

	@Override
	public String provideClassByValue(Object value) {
		return subclassesByValue.get( value );
	}

	@Override
	public String getSqlValue() {
		return sqlValue;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public BasicType<?> getType() {
		return discriminatorType;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public boolean isForced() {
		return forced;
	}

	@Override
	public boolean isNeeded() {
		return needed;
	}
	
	@Override
	public Map<Object, String> getSubclassByDiscriminatorValue() {
		return subclassesByValue;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "ColumnBasedDiscriminator [alias=" );
		builder.append( alias );
		builder.append( ", columnName=" );
		builder.append( columnName );
		builder.append( ", discriminatorType=" );
		builder.append( discriminatorType );
		builder.append( ", forced=" );
		builder.append( forced );
		builder.append( ", needed=" );
		builder.append( needed );
		builder.append( ", sqlValue=" );
		builder.append( sqlValue );
		builder.append( ", value=" );
		builder.append( value );
		builder.append( ", subclassByValue=" );
		builder.append( subclassesByValue );
		builder.append( "]" );
		return builder.toString();
	}

}
