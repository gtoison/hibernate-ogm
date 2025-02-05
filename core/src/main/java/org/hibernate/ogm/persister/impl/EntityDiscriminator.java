/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.DiscriminatorHelper;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * Recognizes the class of an entity in a hierarchy.
 *
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
interface EntityDiscriminator {

	String provideClassByValue(Object value);

	String getSqlValue();

	String getColumnName();

	String getAlias();

	BasicType<?> getType();

	Object getValue();

	/*
	 * NOTE: At the moment OGM is not using this value since the discriminator is used only to create the right class.
	 * This might change in the future if we decide to filter the tuple result when the expected discriminator is
	 * different. When that will be the case, isForced() will needed in conjunction with isInherited() (check
	 * org.hibernate.persister.entity.SingleTableEntityPersister#needsDiscriminator()).
	 */
	boolean isForced();

	boolean isNeeded();

	/**
	 * @see DiscriminatorHelper
	 */
	default BasicType<?> getDiscriminatorType(PersistentClass persistentClass) {
		Type discriminatorType = persistentClass.getDiscriminator().getType();
		if ( discriminatorType instanceof BasicType ) {
			return (BasicType<?>) discriminatorType;
		}
		else {
			throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
		}
	}

	/**
	 * @see AbstractEntityPersister#getSubclassByDiscriminatorValue()
	 */
	Map<Object, String> getSubclassByDiscriminatorValue();
}
