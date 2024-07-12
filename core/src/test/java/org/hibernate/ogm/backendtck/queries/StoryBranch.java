/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import java.util.List;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class StoryBranch {

	// Analyze.NO for filtering in query
	// Store.YES for projection in query
	@GenericField(projectable = Projectable.YES)
	private String storyText;

	@Embedded
	@IndexedEmbedded
	private Ending ending;

	@ElementCollection
	@IndexedEmbedded
	private List<Ending> additionalEndings;

	public StoryBranch() {
	}

	public StoryBranch(String embeddedString) {
		this.storyText = embeddedString;
	}

	public StoryBranch(String storyText, Ending endBranch) {
		this.storyText = storyText;
		this.ending = endBranch;
	}

	public String getStoryText() {
		return storyText;
	}

	public void setStoryText(String storyText) {
		this.storyText = storyText;
	}

	public Ending getEnding() {
		return ending;
	}

	public void setEnding(Ending anotherEmbeddable) {
		this.ending = anotherEmbeddable;
	}

	public List<Ending> getAdditionalEndings() {
		return additionalEndings;
	}

	public void setAdditionalEndings(List<Ending> anotherCollection) {
		this.additionalEndings = anotherCollection;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( ending == null ) ? 0 : ending.hashCode() );
		result = prime * result + ( ( storyText == null ) ? 0 : storyText.hashCode() );
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
		StoryBranch other = (StoryBranch) obj;
		if ( ending == null ) {
			if ( other.ending != null ) {
				return false;
			}
		}
		else if ( !ending.equals( other.ending ) ) {
			return false;
		}
		if ( storyText == null ) {
			if ( other.storyText != null ) {
				return false;
			}
		}
		else if ( !storyText.equals( other.storyText ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "StoryBranch [storyText=" );
		builder.append( storyText );
		builder.append( ", ending=" );
		builder.append( ending );
		builder.append( ", additionalEndings=" );
		builder.append( additionalEndings );
		builder.append( "]" );
		return builder.toString();
	}
}
