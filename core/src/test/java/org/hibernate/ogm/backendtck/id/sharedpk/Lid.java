/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.sharedpk;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Gunnar Morling
 */
@Entity
public class Lid {

	private String id;
	private String color;
	private CoffeeMug mug;

	@Id
	@GeneratedValue(generator = "sharedPkGen")
	@GenericGenerator(name = "sharedPkGen", strategy = "foreign", parameters = @Parameter(name = "property", value = "mug"))
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@OneToOne
	@PrimaryKeyJoinColumn
	public CoffeeMug getMug() {
		return mug;
	}

	public void setMug(CoffeeMug mug) {
		this.mug = mug;
	}
}
