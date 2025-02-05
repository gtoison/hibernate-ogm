/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
class MappedLink {

	@Id
	private String id;

	@ManyToOne
	private MappedNode source;

	@ManyToOne
	private MappedNode target;

	public MappedLink() {
	}

	public MappedLink(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MappedNode getSource() {
		return source;
	}

	void setSource(MappedNode source) {
		this.source = source;
	}

	public MappedNode getTarget() {
		return target;
	}

	void setTarget(MappedNode target) {
		this.target = target;
	}

	public void assignTarget(MappedNode target) {
		setTarget( target );
	}
}
