/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.depositor;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(value = "INSTANT_ACCESS_ACCOUNT")
public class InstantAccessAccount extends Account {

	protected InstantAccessAccount() {
	}

	public InstantAccessAccount(final Depositor depositor) {
		super( depositor );
	}
}
