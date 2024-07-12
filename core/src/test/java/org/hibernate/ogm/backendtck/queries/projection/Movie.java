/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import java.util.Objects;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Movie")
@Indexed
public class Movie {

	@Id
	private Integer id;

	@GenericField(sortable = Sortable.YES)
	private String name;

	@GenericField(sortable = Sortable.YES)
	private String author;

	@GenericField(sortable = Sortable.YES)
	private Integer year;

	public Movie() {
	}

	public Movie(Integer id, String name, String author, int year) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.year = year;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Movie{" +
				"id=" + id +
				", name='" + name + '\'' +
				", author='" + author + '\'' +
				", year=" + year +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Movie movie = (Movie) o;
		return Objects.equals( id, movie.id ) &&
			Objects.equals( name, movie.name ) &&
			Objects.equals( author, movie.author ) &&
			Objects.equals( year, movie.year );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name, author, year );
	}
}
