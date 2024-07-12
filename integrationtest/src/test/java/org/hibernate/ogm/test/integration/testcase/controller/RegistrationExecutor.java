/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.testcase.controller;

import java.util.List;
import java.util.Optional;

import org.hibernate.ogm.test.integration.testcase.model.Member;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import jakarta.persistence.EntityManager;

/**
 * Methods to execute the registration and search of {@link Member}s
 *
 * @author Davide D'Alto
 */
public class RegistrationExecutor {

	private RegistrationExecutor() {
	}

	public static void register(EntityManager em, Member newMember) {
		em.persist( newMember );
	}

	public static Member find(EntityManager em, Long id) {
		return em.find( Member.class, id );
	}

	@SuppressWarnings("unchecked")
	public static void deleteAll(EntityManager em) {
		List<Member> members = em.createQuery( "select t from " + Member.class.getName() + " t" ).getResultList();
		for ( Member member : members ) {
			em.remove( member );
		}
	}

	public static Member findWithNativeQuery(EntityManager em, String nativeQuery) {
		return (Member) em.createNativeQuery( nativeQuery, Member.class ).getSingleResult();
	}

	public static Member findWithQuery(EntityManager em, Long id) {
		return em.createQuery( "FROM Member WHERE id = :id", Member.class )
				.setParameter( "id", id )
				.getSingleResult();
	}

	public static Member findWithEmail(EntityManager em, String email) {
		SearchSession ftem = Search.session( em );
		Optional<Member> uniqueResult = ftem
				.search( Member.class )
				.where( f -> f.wildcard().field( "email" ).matching( email ) )
				.fetchSingleHit();
		
		return uniqueResult.get();
	}
}
