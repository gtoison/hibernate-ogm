/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hsearch;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.Test;

import jakarta.persistence.EntityManager;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SkipByGridDialect(value = { GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE }, comment = "Neo4j is not compatible with HSEARCH 5")
public class HibernateSearchAtopOgmTest extends OgmJpaTestCase {

	@Test
	public void testHibernateSearchJPAAPIUsage() throws Exception {
		EntityManager entityManager = getFactory().createEntityManager();
		final SearchSession ftem = Search.session( entityManager );
		entityManager.getTransaction().begin();
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		entityManager.persist( insurance );
		entityManager.getTransaction().commit();

		entityManager.clear();

		entityManager.getTransaction().begin();
		
		final List<Insurance> resultList = ftem.search( Insurance.class )
				.where( f -> f.match().field( "name" ).matching( "Macif" ) )
				.fetchAllHits();
		
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			entityManager.remove( e );
		}
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void testHibernateSearchNativeAPIUsage() throws Exception {
		final EntityManager entityManager = getFactory().createEntityManager();
		Session session = entityManager.unwrap( Session.class );
		final SearchSession ftSession = Search.session( session );
		entityManager.getTransaction().begin();
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		entityManager.persist( insurance );
		entityManager.getTransaction().commit();

		entityManager.clear();

		entityManager.getTransaction().begin();
		final List<Insurance> resultList = ftSession.search( Insurance.class )
				.where( f -> f.match().field( "name" ).matching( "Macif" ) )
				.fetchAllHits();
		
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			session.delete( e );
		}
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		TestHelper.enableCountersForInfinispan( info.getProperties() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class };
	}
}
