/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import org.bson.types.ObjectId;
import org.hibernate.ogm.backendtck.queries.StoryBranch;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Davide D'Alto
 */
@Entity
public class EntityWithObjectIdAndEmbeddable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private ObjectId id;

	@Embedded
	private StoryBranch anEmbeddable;

	public EntityWithObjectIdAndEmbeddable() {
	}

	public EntityWithObjectIdAndEmbeddable(StoryBranch anEmbeddable) {
		this.anEmbeddable = anEmbeddable;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public StoryBranch getAnEmbeddable() {
		return anEmbeddable;
	}

	public void setAnEmbeddable(StoryBranch details) {
		this.anEmbeddable = details;
	}
}
