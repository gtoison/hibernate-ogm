/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import org.hibernate.ogm.backendtck.callbacks.PreRemovableBus.PreRemovableBusEventListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PreRemove;

@Entity
@EntityListeners(PreRemovableBusEventListener.class)
public class PreRemovableBus extends Bus {

	private boolean preRemoveInvoked;
	private boolean preRemoveInvokedByListener;

	public boolean isPreRemoveInvoked() {
		return preRemoveInvoked;
	}

	public void setPreRemoveInvoked(boolean preRemoveInvoked) {
		this.preRemoveInvoked = preRemoveInvoked;
	}

	public boolean isPreRemoveInvokedByListener() {
		return preRemoveInvokedByListener;
	}

	public void setPreRemoveInvokedByListener(boolean preRemoveInvokedByListener) {
		this.preRemoveInvokedByListener = preRemoveInvokedByListener;
	}

	@PreRemove
	public void preRemove() {
		this.preRemoveInvoked = true;
	}

	public static class PreRemovableBusEventListener {

		@PreRemove
		public void preRemove(PreRemovableBus bus) {
			bus.setPreRemoveInvokedByListener( true );
		}
	}
}
