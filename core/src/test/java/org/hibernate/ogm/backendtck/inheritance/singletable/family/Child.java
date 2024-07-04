/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("CHILD")
@Indexed
class Child extends Person {

	@GenericField
	private String favouriteToy;

	@OneToOne
	private Woman mother;

	@OneToOne
	private Man father;

	public Child() {
	}

	public Child(String name, String favouriteToy) {
		super( name );
		this.favouriteToy = favouriteToy;
	}

	public String getFavouriteToy() {
		return favouriteToy;
	}

	public void setFavouriteToy(String favouriteToy) {
		this.favouriteToy = favouriteToy;
	}

	public Man getFather() {
		return father;
	}

	public void setFather(Man father) {
		this.father = father;
	}

	public Woman getMother() {
		return mother;
	}

	public void setMother(Woman mother) {
		this.mother = mother;
	}
}
