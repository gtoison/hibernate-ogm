/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;

/**
 * Represents a date
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DateType extends AbstractGenericBasicType<Date> {

	public static DateType INSTANCE = new DateType();

	public DateType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, JdbcDateJavaType.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "date";
	}
}
