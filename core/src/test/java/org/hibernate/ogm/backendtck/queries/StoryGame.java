/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Simulate a multi-path story game.
 * It can be used to test embeddable elements and collections.
 *
 * @author Davide D'Alto
 */
@Indexed
@Entity
public class StoryGame {

	@Id
	private Long id;

	@Embedded
	@IndexedEmbedded
	private StoryBranch goodBranch;

	@Embedded
	@IndexedEmbedded
	private StoryBranch evilBranch;

	@ElementCollection
	@IndexedEmbedded
	private List<OptionalStoryBranch> chaoticBranches;

	@ElementCollection
	@IndexedEmbedded
	private List<OptionalStoryBranch> neutralBranches;

	@ElementCollection
	@IndexedEmbedded
	private List<String> dwarves = new ArrayList<String>();

	public StoryGame() {
	}

	public StoryGame(Long id, StoryBranch goodBranch) {
		this.id = id;
		this.goodBranch = goodBranch;
	}

	public Long getId() {
		return id;
	}

	public void setGoodBranch(StoryBranch goodBranch) {
		this.goodBranch = goodBranch;
	}

	public StoryBranch getGoodBranch() {
		return goodBranch;
	}

	public StoryBranch getEvilBranch() {
		return evilBranch;
	}

	public void setEvilBranch(StoryBranch evilBranch) {
		this.evilBranch = evilBranch;
	}

	public List<OptionalStoryBranch> getChaoticBranches() {
		return chaoticBranches;
	}

	public void setChaoticBranches(List<OptionalStoryBranch> chaoticBranchhes) {
		this.chaoticBranches = chaoticBranchhes;
	}

	public List<OptionalStoryBranch> getNeutralBranches() {
		return neutralBranches;
	}

	public void setNeutralBranches(List<OptionalStoryBranch> neutralBranches) {
		this.neutralBranches = neutralBranches;
	}

	public List<String> getDwarves() {
		return dwarves;
	}

	public void setDwarves(List<String> dwarves) {
		this.dwarves = dwarves;
	}
}
