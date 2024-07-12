/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.configuration.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;

/**
 * @author Hardy Ferentschik
 */
public class MongoDBConfigurationTest {
	private Map<String, String> configProperties;
	private ConfigurationPropertyReader propertyReader;
	private OptionsContext globalOptions;

	@Before
	public void setUp() {
		configProperties = new HashMap<>();
		configProperties.put( OgmProperties.DATABASE, "foo" );

		propertyReader = new ConfigurationPropertyReader( configProperties, new ClassLoaderServiceImpl() );
		globalOptions = OptionsContextImpl.forGlobal( OptionValueSources.getDefaultSources( propertyReader ) );
	}

	@Test
	public void testDefaultWriteConcernGetsApplied() {
		MongoClientSettings clientOptions = createMongoClientSettings();

		assertEquals( "Unexpected write concern", WriteConcern.ACKNOWLEDGED, clientOptions.getWriteConcern() );
	}

	@Test
	public void testDefaultReadConcernGetsApplied() {
		MongoClientSettings clientOptions = createMongoClientSettings();

		assertEquals( "Unexpected read concern", ReadConcern.DEFAULT, clientOptions.getReadConcern() );
	}

	@Test
	public void testDefaultServerSelectionTimeoutIsApplied() {
		MongoClientSettings clientOptions = createMongoClientSettings();

		assertEquals(
				"Unexpected server selection timeout",
				MongoClientSettings.builder().build().getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS),
				clientOptions.getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS)
		);
	}

	@Test
	public void testCustomServerSelectionTimeoutIsApplied() {
		int expectedTimeout = 1000;
		configProperties.put( "hibernate.ogm.mongodb.driver.serverSelectionTimeout", String.valueOf( expectedTimeout ) );
		MongoClientSettings clientOptions = createMongoClientSettings();

		assertEquals(
				"Unexpected server selection timeout",
				expectedTimeout,
				clientOptions.getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS)
		);
	}

	private MongoClientSettings createMongoClientSettings() {
		MongoDBConfiguration mongoConfig = new MongoDBConfiguration(
				propertyReader,
				globalOptions
		);
		assertNotNull( mongoConfig );

		MongoClientSettings clientOptions = mongoConfig.buildOptions();
		assertNotNull( clientOptions );
		return clientOptions;
	}

}
