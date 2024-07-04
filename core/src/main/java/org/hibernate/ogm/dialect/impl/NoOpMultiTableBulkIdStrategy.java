/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

/**
 * A {@link MultiTableBulkIdStrategy} that does notghing.
 * <p>
 * In Hibernate OGM we don't support cases where the entity is split in multiple table because
 * some datastores don't have join operations.
 * <p>
 * This class was introduced because Hibernate ORM sometimes needs an implementation anyway
 * and having the default one would cause exceptions.
 *
 * @author Davide D'Alto
 */
public class NoOpMultiTableBulkIdStrategy {
}
