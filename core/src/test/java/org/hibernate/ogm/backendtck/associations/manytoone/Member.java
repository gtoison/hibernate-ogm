/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Member {
	private String id;
	private String name;
	private JUG memberOf;

	public Member() {
	}

	public Member(String id) {
		this.id = id;
	}

	@Id
	@Column(name = "member_id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public JUG getMemberOf() {
		return memberOf;
	}

	public void setMemberOf(JUG memberOf) {
		this.memberOf = memberOf;
	}

}
