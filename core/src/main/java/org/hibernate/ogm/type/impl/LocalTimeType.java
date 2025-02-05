/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.time.LocalTime;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;

/**
 * Uses a {@link TimeGridTypeDescriptor} instead of {@link PassThroughGridTypeDescriptor},
 * because at the moment {@link LocalTimeJavaType} is not able to unwrap {@link LocalTime}.
 *
 * @author Fabio Massimo Ercoli
 */
public class LocalTimeType extends AbstractGenericBasicType<LocalTime> {
	public static final LocalTimeType INSTANCE = new LocalTimeType();

	public LocalTimeType() {
		super( TimeGridTypeDescriptor.INSTANCE, LocalTimeJavaType.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return null;
	}

}
