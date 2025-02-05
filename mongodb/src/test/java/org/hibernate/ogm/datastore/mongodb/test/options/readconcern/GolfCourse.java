/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readconcern;

import org.hibernate.ogm.datastore.mongodb.options.ReadConcern;
import org.hibernate.ogm.datastore.mongodb.options.ReadConcernType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Gunnar Morling
 * @author Aleksandr Mylnikov
 */
@Entity
@ReadConcern(ReadConcernType.LINEARIZABLE)
public class GolfCourse {

	private long id;
	private String name;

	public GolfCourse(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public GolfCourse() {
	}

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "GolfCourse [id=" + id + ", name=" + name + "]";
	}
}
