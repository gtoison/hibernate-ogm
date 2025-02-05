/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.infinispanembedded;

import org.hibernate.ogm.test.integration.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using Infinispan.
 *
 * @author Davide D'Alto
 */
@RunWith(Arquillian.class)
public class InfinispanModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
			.Builder( InfinispanModuleMemberRegistrationIT.class )
			.persistenceXml( persistenceXml() )
			.manifestDependencies( "org.hibernate.ogm:${module-slot.org.hibernate.ogm.short-id} services, org.hibernate.ogm.infinispan-embedded:${module-slot.org.hibernate.ogm.short-id} services" )
			.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.getOrCreateProperties()
				.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "infinispan_embedded" ).up()
				.createProperty().name( "hibernate.search.backend.directory.type" ).value( "local-heap" ).up()
				.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${module-slot.org.hibernate.search.short-id}" ).up()
			.up().up();
	}

}
