/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

/**
 * @author Gunnar Morling
 */
@Entity
public class GuitarPlayer {

	private long id;
	private String name;

	GuitarPlayer() {
	}

	public GuitarPlayer(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "guitarGen")
	@TableGenerator(name = "guitarGen", table = "GuitarPlayerSequence", pkColumnValue = "guitarPlayer", valueColumnName = "nextGuitarPlayerId", allocationSize = 1)
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
}
