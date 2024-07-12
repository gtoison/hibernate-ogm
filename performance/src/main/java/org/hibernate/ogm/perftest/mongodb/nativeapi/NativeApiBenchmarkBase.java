/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.nativeapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Base class for MongoDB native API benchmarks.
 *
 * @author Gunnar Morling
 */
public class NativeApiBenchmarkBase {

	private static Properties properties = new Properties();

	static {
		try ( InputStream resourceAsStream = NativeApiBenchmarkBase.class.getClassLoader().getResourceAsStream( "native-settings.properties" ) ) {
			properties.load( resourceAsStream );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@State(Scope.Benchmark)
	public static class ClientHolder {

		MongoClient mongo;
		MongoDatabase db;
		Random rand;

		@Setup
		public void setupDatastore() throws Exception {
			MongoClient mongo = getMongoClient();
			db = mongo.getDatabase( properties.getProperty( "database" ) );
			db.drop();
			rand = new Random();
		}
	}

	protected static MongoClient getMongoClient() throws UnknownHostException {
		ServerAddress serverAddress = new ServerAddress( properties.getProperty( "host" ), 27017 );

		MongoClientSettings.Builder optionsBuilder = MongoClientSettings.builder()
				.applyToClusterSettings(b -> b.hosts(Collections.singletonList( serverAddress )))
				.applyToSocketSettings(b -> b.connectTimeout( 1000, TimeUnit.MILLISECONDS ));

		optionsBuilder.writeConcern( WriteConcern.ACKNOWLEDGED );
		optionsBuilder.readPreference( ReadPreference.primary() );

		MongoClientSettings clientOptions = optionsBuilder.build();

		MongoClient mongo = MongoClients.create( clientOptions );

		return mongo;
	}
}
