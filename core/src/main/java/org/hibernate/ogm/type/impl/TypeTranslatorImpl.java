/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;

import org.hibernate.mapping.SimpleValue;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.type.descriptor.impl.AttributeConverterGridTypeDescriptorAdaptor;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.CalendarDateJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * @author Emmanuel Bernard
 * @author Nicolas Helleringer
 */
public class TypeTranslatorImpl implements TypeTranslator {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	// ORM Type to OGM GridType relation
	private final Map<JavaType<?>, GridType> typeConverter;
	private final GridDialect dialect;
	// ORM type resolver
	private final BasicTypeRegistry typeResolver;
	private final JavaTypeRegistry javaTypeRegistry;

	public TypeTranslatorImpl(GridDialect dialect, TypeConfiguration typeConfiguration) {
		this.dialect = dialect;
		this.typeResolver = typeResolver;

		Map<JavaType<?>, GridType> tmpMap = newHashMap( 20 );
		tmpMap.put( ClassJavaType.INSTANCE, ClassType.INSTANCE );
		tmpMap.put( LongJavaType.INSTANCE, LongType.INSTANCE );
		tmpMap.put( IntegerJavaType.INSTANCE, IntegerType.INSTANCE );
		tmpMap.put( DoubleJavaType.INSTANCE, DoubleType.INSTANCE );
		tmpMap.put( FloatJavaType.INSTANCE, FloatType.INSTANCE );
		tmpMap.put( ShortJavaType.INSTANCE, ShortType.INSTANCE );
		tmpMap.put( CharacterJavaType.INSTANCE, CharacterType.INSTANCE );
		tmpMap.put( StringJavaType.INSTANCE, StringType.INSTANCE );
		tmpMap.put( UrlJavaType.INSTANCE, UrlType.INSTANCE );
		tmpMap.put( BigDecimalJavaType.INSTANCE, BigDecimalType.INSTANCE );
		tmpMap.put( BigIntegerJavaType.INSTANCE, BigIntegerType.INSTANCE );
		tmpMap.put( BooleanJavaType.INSTANCE, BooleanType.INSTANCE );
		tmpMap.put( TrueFalseJavaType.INSTANCE, TrueFalseType.INSTANCE );
		tmpMap.put( YesNoJavaType.INSTANCE, YesNoType.INSTANCE );
		tmpMap.put( NumericBooleanJavaType.INSTANCE, NumericBooleanType.INSTANCE );
		tmpMap.put( ByteJavaType.INSTANCE, ByteType.INSTANCE );
		tmpMap.put( DateJavaType.INSTANCE, DateType.INSTANCE );
		tmpMap.put( TimestampJavaType.INSTANCE, TimestampType.INSTANCE );
		tmpMap.put( TimeJavaType.INSTANCE, TimeType.INSTANCE );
		tmpMap.put( CalendarDateJavaType.INSTANCE, CalendarDateType.INSTANCE );
		tmpMap.put( CalendarJavaType.INSTANCE, CalendarType.INSTANCE );
		tmpMap.put( BinaryJavaType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( MaterializedBlobJavaType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( MaterializedClobJavaType.INSTANCE, StringType.INSTANCE );
		tmpMap.put( ImageJavaType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( UUIDBinaryJavaType.INSTANCE, UUIDType.INSTANCE );
		tmpMap.put( UUIDCharJavaType.INSTANCE, UUIDType.INSTANCE );
		tmpMap.put( LocalDateJavaType.INSTANCE, LocalDateType.INSTANCE );
		tmpMap.put( LocalTimeJavaType.INSTANCE, LocalTimeType.INSTANCE );
		tmpMap.put( LocalDateTimeJavaType.INSTANCE, LocalDateTimeType.INSTANCE );

		typeConverter = Collections.unmodifiableMap( tmpMap );
	}

	@Override
	public GridType getType(Type type) {
		if ( type == null ) {
			return null;
		}

		//TODO should we cache results? It seems an actual HashMap might be slower but it makes it more robust
		//     against badly written dialects
		GridType dialectType = dialect.overrideType( type );
		if ( dialectType != null ) {
			return dialectType;
		}
		else if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new SerializableAsByteArrayType<>( exposedType.getJavaTypeDescriptor() );
		}
		else if ( type instanceof ConvertedBasicType<?> ) {
			// Handles JPA AttributeConverter integration logic
			return buildAttributeConverterGridTypeAdaptor( (ConvertedBasicType<?>) type );

		}
		else if ( type instanceof AbstractStandardBasicType ) {
			AbstractStandardBasicType<?> exposedType = (AbstractStandardBasicType<?>) type;
			final GridType gridType = typeConverter.get( exposedType );
			if ( gridType == null ) {
				throw log.unableToFindGridType( exposedType.getName() );
			}
			return gridType;
		}
		else if ( type instanceof CustomType ) {
			CustomType cType = (CustomType) type;
			final UserType userType = cType.getUserType();
			if ( userType instanceof EnumType ) {
				EnumType enumType = (EnumType) userType;
				//should we cache that (the key must be enumClass / isOrdinal
				return new org.hibernate.ogm.type.impl.EnumType( cType, enumType );
			}
			//let it go it will eventually fail
		}
		else if ( type instanceof org.hibernate.type.ComponentType ) {
			org.hibernate.type.ComponentType componentType = (org.hibernate.type.ComponentType) type;
			return new ComponentType( componentType, this );
		}
		else if ( type instanceof org.hibernate.type.ManyToOneType ) {
			//do some stuff
			org.hibernate.type.ManyToOneType manyToOneType = (org.hibernate.type.ManyToOneType) type;
			return new ManyToOneType( manyToOneType, this );
		}
		else if ( type instanceof org.hibernate.type.OneToOneType ) {
			//do some stuff
			org.hibernate.type.OneToOneType oneToOneType = (org.hibernate.type.OneToOneType) type;
			return new OneToOneType( oneToOneType, this );
		}
		else if ( type instanceof org.hibernate.type.CollectionType ) {
			return new CollectionType( (org.hibernate.type.CollectionType) type );
		}
		throw log.unableToFindGridType( type.getClass().getName() );
	}

	/**
	 * Logic modeled after {@link SimpleValue#buildAttributeConverterTypeAdapter}
	 * <p>
	 * Adapt AttributeConverter to GridType. Most of the logic is done by the
	 * AttributeConverterGridTypeDescriptorAdaptor class which will call the attribute converter and then call the
	 * GridType compliant with the intermediary type
	 */
	private <T> AttributeConverterGridTypeAdaptor<T> buildAttributeConverterGridTypeAdaptor(ConvertedBasicType<T> specificType) {
		BasicValueConverter<T, ?> attributeConverter = specificType.getValueConverter();
		JavaType<T> converterJavaTypeDescriptor = attributeConverter.getDomainJavaType();

		// Rebuild the definition as we need some generic type extraction logic from it
		final Class<?> databaseColumnJavaType = attributeConverter.getRelationalJavaType().getJavaTypeClass();

		// Find the GridType for the intermediary datastore Java type (from the attribute converter
		Type intermediaryORMType = typeResolver.getRegisteredType( databaseColumnJavaType.getName() );
		if ( intermediaryORMType == null ) {
			throw log.cannotFindTypeForAttributeConverter( specificType.getValueConverter().getClass(), databaseColumnJavaType );
		}
		GridType intermediaryOGMGridType = this.getType( intermediaryORMType );

		// find the JavaTypeDescriptor representing the "intermediate database type representation".
		final BasicJavaType<?> intermediateJavaTypeDescriptor = javaTypeRegistry.resolveDescriptor( intermediaryORMType );
		// and finally construct the adapter, which injects the AttributeConverter calls into the binding/extraction
		// 		process...
		final GridTypeDescriptor gridTypeDescriptorAdapter = new AttributeConverterGridTypeDescriptorAdaptor(
				attributeConverter,
				intermediaryOGMGridType,
				intermediateJavaTypeDescriptor
		);
		return new AttributeConverterGridTypeAdaptor<T>( specificType, gridTypeDescriptorAdapter );
	}
}
