/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Gunnar Morling
 */
@Entity
public class GrandMother {

	private String id;
	private List<GrandChild> grandChildren = new ArrayList<GrandChild>();

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ElementCollection
	@OrderColumn(name = "birthorder")
	public List<GrandChild> getGrandChildren() {
		return grandChildren;
	}

	public void setGrandChildren(List<GrandChild> children) {
		this.grandChildren = children;
	}
}
