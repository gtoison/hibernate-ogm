/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.ordercolumn;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

/**
 * @author Gunnar Morling
 */
@Entity
public class Episode {

	private String id;
	private String name;
	private TvShow show;
	private List<Writer> authors = new ArrayList<Writer>();

	Episode() {
	}

	public Episode(String id, String name, TvShow show) {
		this.id = id;
		this.name = name;
		this.show = show;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn(insertable = false, updatable = false, name = "tv_show_id")
	public TvShow getShow() {
		return show;
	}

	public void setShow(TvShow show) {
		this.show = show;
	}

	@ManyToMany
	@JoinTable(
			joinColumns = @JoinColumn(name = "episodeId"),
			inverseJoinColumns = @JoinColumn(name = "authorId")
	)
	@OrderColumn(name = "authorOrder")
	public List<Writer> getAuthors() {
		return authors;
	}

	public void setAuthors(List<Writer> authors) {
		this.authors = authors;
	}
}
