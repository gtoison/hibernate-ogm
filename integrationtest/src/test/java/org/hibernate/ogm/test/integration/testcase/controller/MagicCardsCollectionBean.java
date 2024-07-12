/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.testcase.controller;

import java.util.List;

import javax.ejb.Stateful;

import org.hibernate.ogm.test.integration.testcase.model.MagicCard;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateful
public class MagicCardsCollectionBean {

	@PersistenceContext
	public EntityManager em;

	public void storeCard(MagicCard card) {
		em.persist( card );
	}

	public MagicCard loadById(Long id) {
		return em.find( MagicCard.class, id );
	}

	public List<MagicCard> findByName(String name) {
		SearchSession SearchSession = Search.session( em );
		
		return SearchSession
				.search( MagicCard.class )
				.where( f -> f.match().field( "name" ).matching( name ) )
				.fetchAllHits();
	}
}
