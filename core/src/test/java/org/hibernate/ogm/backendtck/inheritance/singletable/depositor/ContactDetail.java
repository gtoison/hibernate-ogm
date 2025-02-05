/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.depositor;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CONTACT_DETAIL")
public class ContactDetail {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;


	@Column(name = "TYPE", length = 50, updatable = false)
	@Basic(optional = false)
	@Enumerated(EnumType.STRING)
	private ContactType type;

	@Column(name = "VALUE", length = 50)
	@Basic(optional = false)
	private String value;

	protected ContactDetail() {
		// for JPA
	}

	public ContactDetail(final ContactType type, final String value) {
		this.type = type;
		this.value = value;
	}

	public ContactType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

}
