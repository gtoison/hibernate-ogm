/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author The Viet Nguyen
 */
@TestForIssue(jiraKey = "OGM-1222")
public class InsertTest extends OgmTestCase {

	/**
	 * Verify flush does not duplicate Entity on changing field, even if we are in
	 * the same transaction that creates the Entity itself.
	 */
	@Test
	public void testModifyObjectAfterPersisting() {
		inTransaction( em -> {
			Subject subject = new Subject( "1", "name" );
			em.persist( subject );
			subject.setName( "name2" );
			em.flush();
		} );

		inTransaction( em -> {
			Subject found = (Subject) em.createQuery( "FROM Subject" ).getSingleResult();
			assertThat( found ).isNotNull();
			assertThat( found.getName() ).isEqualTo( "name2" );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Subject.class };
	}

	@Entity(name = "Subject")
	@Indexed
	static class Subject {

		@Id
		private String id;

		@GenericField
		private String name;

		public Subject() {
		}

		public Subject(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
