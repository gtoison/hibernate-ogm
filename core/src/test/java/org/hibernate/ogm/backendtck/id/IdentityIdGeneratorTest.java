/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.hibernate.HibernateException;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.Throwables;
import org.hibernate.ogm.utils.jpa.SingleJpaTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jakarta.persistence.EntityManager;

/**
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
public class IdentityIdGeneratorTest extends SingleJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Override
	@Before
	public void createFactory() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000065" );
		try {
			super.createFactory();
			fail( "Expected session factory set-up to fail as IDENTITY columns are not supported" );
		}
		catch (Exception e) {
			throw Throwables.getRootCause( e );
		}
	}

	@Test
	@SkipByGridDialect(value = GridDialectType.MONGODB, comment = "MongoDB supports IDENTITY columns, but not of type Long.")
	public void testIdentityGenerator() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		Animal jungleKing = new Animal();
		Animal fish = new Animal();

		jungleKing.setName( "Lion" );
		jungleKing.setSpecies( "Mammal" );
		em.persist( jungleKing );

		fish.setName( "Shark" );
		fish.setSpecies( "Tiger Shark" );
		em.persist( fish );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		Animal animal = em.find( Animal.class, jungleKing.getId() );
		assertThat( animal ).isNotNull();
		assertThat( animal.getId() ).isEqualTo( 1 );
		assertThat( animal.getName() ).isEqualTo( "Lion" );
		em.remove( animal );

		animal = em.find( Animal.class, fish.getId() );
		assertThat( animal ).isNotNull();
		assertThat( animal.getId() ).isEqualTo( 2 );
		assertThat( animal.getName() ).isEqualTo( "Shark" );
		em.remove( animal );
		em.getTransaction().commit();

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Animal.class
		};
	}
}
