/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.bson.types.ObjectId;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Gunnar Morling
 */
@Entity
public class MusicGenre {

	private ObjectId id;
	private String name;
	private Set<Bar> playedIn = new HashSet<Bar>();

	MusicGenre() {
	}

	MusicGenre(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "musicGenre")
	public Set<Bar> getPlayedIn() {
		return playedIn;
	}

	public void setPlayedIn(Set<Bar> playedIn) {
		this.playedIn = playedIn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		MusicGenre other = (MusicGenre) obj;
		return Objects.equals( name, other.name );
	}
}
