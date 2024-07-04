/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.backendtck.simpleentity.Hero;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

/**
 * @author Jonathan Wood
 */
@Entity
class HeroClub {

	@Id
	private String name;

	@OneToMany
	@OrderColumn
	private List<Hero> members;

	public HeroClub() {
		this.members = new ArrayList<Hero>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Hero> getMembers() {
		return members;
	}

	public void setMembers(List<Hero> members) {
		this.members = members;
	}

}
