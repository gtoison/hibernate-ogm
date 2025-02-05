/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.jpa;

import static org.fest.assertions.Fail.fail;

import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class JPAStandaloneNoOGMTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/no-ogm.xml", Poem.class );

	@Test
	public void testJTAStandaloneNoOgm() throws Exception {
		EntityManagerFactory emf = null;

		// Failure is expected as we didn't configure a JDBC connection nor a Dialect
		// (and this would fail only if effectively loading Hibernate ORM without OGM superpowers)

		try {
			emf = Persistence.createEntityManagerFactory( "withJtaDataSourcePU" );
			fail( "Expected exception was not raised" );
		}
		catch ( PersistenceException pe ) {
			//ORM will throw some exception; verifying details would be too specific.
		}

		if ( emf != null ) {
			emf.close(); // should not be reached, but cleanup in case the test fails.
		}
	}
}
