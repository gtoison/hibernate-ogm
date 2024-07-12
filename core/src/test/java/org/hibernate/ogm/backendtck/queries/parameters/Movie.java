/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.parameters;

import java.util.Date;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class Movie {

	@Id
	private String id;

	@Enumerated(EnumType.ORDINAL)
	@GenericField(projectable = Projectable.YES)
	private Genre genre;

	private String title;

	@GenericField(projectable = Projectable.YES)
	@Convert(converter = YesNoConverter.class)
	private boolean suitableForKids;

	@Temporal(TemporalType.DATE)
	@GenericField(projectable = Projectable.YES)
	private Date releaseDate;

	@GenericField(projectable = Projectable.YES)
	private byte viewerRating;

	Movie() {
	}

	public Movie(String id) {
		this.id = id;
	}

	public Movie(String id, Genre genre, String title, boolean suitableForKids, Date releaseDate, byte viewerRating) {
		this.id = id;
		this.genre = genre;
		this.title = title;
		this.suitableForKids = suitableForKids;
		this.releaseDate = releaseDate;
		this.viewerRating = viewerRating;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public boolean isSuitableForKids() {
		return suitableForKids;
	}

	public void setSuitableForKids(boolean suitableForKids) {
		this.suitableForKids = suitableForKids;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public byte getViewerRating() {
		return viewerRating;
	}

	public void setViewerRating(byte viewerRating) {
		this.viewerRating = viewerRating;
	}
}
