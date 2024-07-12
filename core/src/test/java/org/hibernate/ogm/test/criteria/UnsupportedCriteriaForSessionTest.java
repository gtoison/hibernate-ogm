/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.criteria;

import org.hibernate.Session;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;

/**
 * Test that when a user tries to access {@link CriteriaBuilder} a we throw a meaningful exception.
 *
 * @author Davide D'Alto
 */
public class UnsupportedCriteriaForSessionTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreateCriteriaWithClass() {
		thrown.expect( NotSupportedException.class );
		thrown.expectMessage( "OGM-23 - Criteria queries are not supported yet" );

		inTransaction( em -> {
			HibernateCriteriaBuilder cb = session( em ).getCriteriaBuilder();
			JpaCriteriaQuery<Puppet> query = cb.createQuery( Puppet.class );
			JpaRoot<Puppet> root = query.from( Puppet.class );
			
			query.select( root );
			
			em.createQuery( query ).getResultList();
		} );
	}

	@Test
	public void testEtityManagerGetCriteriaBuilder() {
		thrown.expect( NotSupportedException.class );
		thrown.expectMessage( "OGM-23 - Criteria queries are not supported yet" );

		inTransaction( em -> {
			em.getCriteriaBuilder();
		} );
	}

	@Test
	public void testSessionFactoryGetCriteriaBuilder() {
		thrown.expect( NotSupportedException.class );

		getFactory().getCriteriaBuilder();
	}

	private Session session(EntityManager em) {
		return em.unwrap( Session.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Puppet.class };
	}

	// Not important
	@Entity
	class Puppet {

		@Id
		String name;
	}
}
