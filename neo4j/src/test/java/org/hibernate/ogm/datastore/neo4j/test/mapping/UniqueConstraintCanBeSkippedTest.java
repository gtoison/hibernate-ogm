/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

/**
 * Test that the creation of unique constraints is skipped when
 * {@link Environment#UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY} is set to
 * {@link  UniqueConstraintSchemaUpdateStrategy#SKIP}
 *
 * @author Davide D'Alto
 */
public class UniqueConstraintCanBeSkippedTest extends Neo4jJpaTestCase {

	private EntityWithConstraints entityWithConstraints;

	@Entity
	static class EntityWithConstraints {
		@Id
		private String id;

		@Column(unique = true)
		private long uniqueColumn;

		public String getId() {
			return id;
		}

		public void setId(String login) {
			this.id = login;
		}

		public long getUniqueColumn() {
			return uniqueColumn;
		}

		public void setUniqueColumn(long insuranceNumber) {
			this.uniqueColumn = insuranceNumber;
		}
	}

	@Before
	public void setup() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		entityWithConstraints = new EntityWithConstraints();
		entityWithConstraints.setId( "id_1" );
		entityWithConstraints.setUniqueColumn( 12345678 );

		em.persist( entityWithConstraints );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void skipUniqueConstraintsGenerationWhenRequired() throws Exception {
		{
			final EntityManager em = getFactory().createEntityManager();
			em.getTransaction().begin();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "id_2" );
			duplicated.setUniqueColumn( entityWithConstraints.getUniqueColumn() );
			em.persist( duplicated );
			em.getTransaction().commit();
			em.close();
		}
		{
			final EntityManager em = getFactory().createEntityManager();
			em.getTransaction().begin();
			EntityWithConstraints entity1 = em.find( EntityWithConstraints.class, "id_1" );
			EntityWithConstraints entity2 = em.find( EntityWithConstraints.class, "id_2" );
			assertThat( entity1.getUniqueColumn() ).isEqualTo( entity2.getUniqueColumn() );
			em.getTransaction().commit();
			em.close();
		}
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.getProperties().put( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, UniqueConstraintSchemaUpdateStrategy.SKIP );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithConstraints.class };
	}
}
