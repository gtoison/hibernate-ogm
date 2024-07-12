/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.type.BasicType;

/**
 * Used when a discriminator is not needed.
 *
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
public class NotNeededDiscriminator implements EntityDiscriminator {

	public static final NotNeededDiscriminator INSTANCE = new NotNeededDiscriminator();

	private NotNeededDiscriminator() {
	}

	@Override
	public String provideClassByValue(Object value) {
		return null;
	}

	@Override
	public String getSqlValue() {
		return null;
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public BasicType<?> getType() {
		return null;
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public boolean isForced() {
		return false;
	}

	@Override
	public boolean isNeeded() {
		return false;
	}

	@Override
	public Map<Object, String> getSubclassByDiscriminatorValue() {
		return Collections.emptyMap();
	}
}
