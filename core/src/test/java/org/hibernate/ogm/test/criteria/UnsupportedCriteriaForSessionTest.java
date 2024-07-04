/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.criteria;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;

/**
 * Test that when a user tries to access {@link Criteria} or {@link CriteriaBuilder} a we throw a meaningful exception.
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
			session( em ).createCriteria( Persistence.class );
		} );
	}

	@Test
	public void testCreateCriteriaWithString() {
		thrown.expect( NotSupportedException.class );
		inTransaction( em -> {
			session( em ).createCriteria( "Puppet" );
		} );
	}

	@Test
	public void testCreateCriteriaWithClassAndAlias() {
		thrown.expect( NotSupportedException.class );
		thrown.expectMessage( "OGM-23 - Criteria queries are not supported yet" );

		inTransaction( em -> {
			session( em ).createCriteria( Puppet.class, "p" );
		} );
	}

	@Test
	public void testCreateCriteriaWithStringAndAlias() {
		thrown.expect( NotSupportedException.class );
		thrown.expectMessage( "OGM-23 - Criteria queries are not supported yet" );

		inTransaction( em -> {
			session( em ).createCriteria( "Puppet", "p" );
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
