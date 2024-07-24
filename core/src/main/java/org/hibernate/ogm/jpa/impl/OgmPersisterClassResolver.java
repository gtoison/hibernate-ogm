/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import org.hibernate.mapping.Collection;
import org.hibernate.ogm.persister.impl.OgmBasicCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmOneToManyPersister;
import org.hibernate.ogm.persister.impl.OgmSingleTableEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.persister.spi.PersisterClassResolver;

/**
 * Return the Ogm persisters
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmPersisterClassResolver extends StandardPersisterClassResolver implements PersisterClassResolver {

	@Override
	public Class<? extends EntityPersister> singleTableEntityPersister() {
		return OgmSingleTableEntityPersister.class;
	}

	@Override
	public Class<? extends EntityPersister> joinedSubclassEntityPersister() {
		throw new UnsupportedOperationException( "Joined subclasses strategy not supported" );
	}

	@Override
	public Class<? extends EntityPersister> unionSubclassEntityPersister() {
		throw new UnsupportedOperationException( "Union subclasses strategy not supported" );
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return metadata.isOneToMany() ? oneToManyPersister() : basicCollectionPersister();
	}

	private Class<OgmOneToManyPersister> oneToManyPersister() {
		return OgmOneToManyPersister.class;
	}

	private Class<OgmBasicCollectionPersister> basicCollectionPersister() {
		return OgmBasicCollectionPersister.class;
	}
}
