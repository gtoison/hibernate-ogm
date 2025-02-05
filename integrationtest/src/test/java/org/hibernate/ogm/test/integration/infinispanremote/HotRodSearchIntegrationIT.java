/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.infinispanremote;

import org.hibernate.ogm.test.integration.testcase.MagiccardsDatabaseScenario;
import org.hibernate.ogm.test.integration.testcase.controller.MagicCardsCollectionBean;
import org.hibernate.ogm.test.integration.testcase.model.MagicCard;
import org.hibernate.ogm.test.integration.testcase.util.ModulesHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test for the combination of Hibernate OGM and Hibernate Search modules on WildFly,
 * using a remote Infinispan instance over Hot Rod.
 *
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class HotRodSearchIntegrationIT extends MagiccardsDatabaseScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, "modules-magic-searchit-hotrod.war" )
				.addClasses( MagicCard.class, MagicCardsCollectionBean.class, HotRodSearchIntegrationIT.class, MagiccardsDatabaseScenario.class );
		String persistenceXml = ModulesHelper.injectVariables( persistenceXml().exportAsString() );
		webArchive.addAsResource( new StringAsset( persistenceXml ), "META-INF/persistence.xml" );
		webArchive.addAsResource( "hotrod-client-testingconfiguration-offset100.properties", "hotrod-client-configuration.properties" );
		ModulesHelper.addModulesDependencyDeclaration( webArchive, "org.hibernate.ogm:${module-slot.org.hibernate.ogm.short-id} services, org.hibernate.ogm.infinispan-remote:${module-slot.org.hibernate.ogm.short-id} services" );
		return webArchive;
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
					.name( "primary" )
					.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
					.getOrCreateProperties()
						.createProperty().name( "hibernate.search.backend.directory.type" ).value( "local-heap" ).up()
						.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "infinispan_remote" ).up()
						.createProperty().name( "hibernate.ogm.infinispan_remote.configuration_resource_name" ).value( "hotrod-client-configuration.properties" ).up()
						.createProperty().name( "hibernate.ogm.cache.transaction.mode" ).value( "NONE" ).up()
						.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${module-slot.org.hibernate.search.short-id}" ).up()
				.up().up();
	}
}
