/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.configuration;

import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheConfiguration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Used a NOT existing cache configuration
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
@CacheConfiguration("notExist")
public class NotExistCacheConfigurationEntity {

	@Id
	private String id;

}
