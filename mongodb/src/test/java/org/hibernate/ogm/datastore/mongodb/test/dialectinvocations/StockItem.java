/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.dialectinvocations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Gunnar Morling
 */
@Entity
public class StockItem {

	private String id;
	private String itemName;
	private int count;

	@Id
	@Column(name = "_id") // TODO make this implicit for MongoDB?
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	@Column(name = "cnt")
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
