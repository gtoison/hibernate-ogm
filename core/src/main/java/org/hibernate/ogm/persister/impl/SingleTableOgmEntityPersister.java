/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * Use single table strategy.
 *
 * @see javax.persistence.InheritanceType#SINGLE_TABLE
 * @author Emmanuel Bernard
 */
public class SingleTableOgmEntityPersister extends OgmEntityPersister {

	public SingleTableOgmEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext, resolveDiscriminator( persistentClass, creationContext.getSessionFactory() ) );
	}

	private static EntityDiscriminator resolveDiscriminator(final PersistentClass persistentClass, final SessionFactoryImplementor factory) {
		if ( persistentClass.isPolymorphic() ) {
			Value discrimValue = persistentClass.getDiscriminator();
			Selectable selectable = discrimValue.getColumns().get(0);
			if ( discrimValue.hasFormula() ) {
				throw new UnsupportedOperationException( "OGM doesn't support discriminator formulas" );
			}
			else {
				return new ColumnBasedDiscriminator( persistentClass, factory, (Column) selectable );
			}
		}
		else {
			return NotNeededDiscriminator.INSTANCE;
		}
	}
}
