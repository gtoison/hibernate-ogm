/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * Use table per class strategy.
 *
 * @see javax.persistence.InheritanceType#TABLE_PER_CLASS
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
public class UnionSubclassOgmEntityPersister extends OgmEntityPersister {

	public UnionSubclassOgmEntityPersister(PersistentClass persistentClass,
			EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext,
				new TablePerClassDiscriminator( persistentClass ) );
	}
}
