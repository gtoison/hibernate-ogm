/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import org.hibernate.ogm.backendtck.callbacks.PreUpdatableBus.PreUpdatableBusEventListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PreUpdate;

@Entity
@EntityListeners(PreUpdatableBusEventListener.class)
public class PreUpdatableBus extends Bus {

	private String field;

	private boolean preUpdated;
	private boolean preUpdatedByListener;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public boolean isPreUpdated() {
		return preUpdated;
	}

	public void setPreUpdated(boolean preUpdated) {
		this.preUpdated = preUpdated;
	}

	public boolean isPreUpdatedByListener() {
		return preUpdatedByListener;
	}

	public void setPreUpdatedByListener(boolean preUpdatedByListener) {
		this.preUpdatedByListener = preUpdatedByListener;
	}

	@PreUpdate
	public void preUpdate() {
		this.preUpdated = true;
	}

	public static class PreUpdatableBusEventListener {

		@PreUpdate
		public void preUpdate(PreUpdatableBus bus) {
			bus.setPreUpdatedByListener( true );
		}
	}
}
