/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.inheritance.singletable;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.ogm.datastore.mongodb.type.impl.StringAsObjectIdType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Node {

	private Set<NodeLink> children;

	private String id;
	private String name;

	public Node() {
		children = new HashSet<>();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Type(StringAsObjectIdType.class)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@OneToMany(mappedBy = "source")
	public Set<NodeLink> getChildren() {
		return children;
	}

	void setChildren(Set<NodeLink> children) {
		this.children = children;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		Node other = (Node) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(" + id + ", " + name + ")";
	}

}
