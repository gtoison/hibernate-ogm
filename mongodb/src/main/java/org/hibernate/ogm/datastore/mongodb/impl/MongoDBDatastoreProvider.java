/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.binarystorage.GridFSFields;
import org.hibernate.ogm.datastore.mongodb.binarystorage.GridFSStorageManager;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.query.parsing.impl.MongoDBBasedQueryParserService;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoDriverInformation;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * Provides access to a MongoDB instance
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class MongoDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private ServiceRegistryImplementor serviceRegistry;

	private MongoClient mongo;
	private MongoDatabase mongoDb;
	private MongoDBConfiguration config;
	private JndiService jndiService;

	private GridFSStorageManager binaryStorageManager;

	public MongoDBDatastoreProvider() {
	}

	/**
	 * Only used in tests.
	 *
	 * @param mongoClient the client to connect to mongodb
	 */
	public MongoDBDatastoreProvider(MongoClient mongoClient) {
		this.mongo = mongoClient;
	}

	@Override
	public void configure(Map configurationValues) {
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, classLoaderService );

		try {
			this.config = new MongoDBConfiguration( propertyReader, optionsService.context().getGlobalOptions() );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToConfigureDatastoreProvider( e );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return MongoDBBasedQueryParserService.class;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return MongoDBSchemaDefiner.class;
	}

	@Override
	public boolean allowsTransactionEmulation() {
		return true;
	}

	@Override
	public void start() {
		if ( config.getNativeClientResource() == null ) {
			startClientAndExtractDatabase();
		}
		else {
			lookupDatabase();
		}

		// clear resources
		this.jndiService = null;
	}

	private void lookupDatabase() {
		try {
			log.tracef( "Retrieving MongoDatabase from JNDI at %1$s", config.getNativeClientResource() );
			mongoDb = (MongoDatabase) jndiService.locate( config.getNativeClientResource() );
		}
		catch (RuntimeException e) {
			throw log.errorOnFetchJndiClientProperty( config.getNativeClientResource() );
		}
	}

	private void startClientAndExtractDatabase() {
		try {
			if ( mongo == null ) {
				mongo = createMongoClient( config );
			}
			mongoDb = extractDatabase( mongo, config );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	protected MongoClient createMongoClient(MongoDBConfiguration config) {
		MongoClientSettings clientOptions = config.buildOptions();
		log.connectingToMongo( config.getHosts().toString(), clientOptions.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS) );
		try {
			List<ServerAddress> serverAddresses = new ArrayList<>( config.getHosts().size() );
			for ( Hosts.HostAndPort hostAndPort : config.getHosts() ) {
				serverAddresses.add( new ServerAddress( hostAndPort.getHost(), hostAndPort.getPort() ) );
			}
			return MongoClients.create( clientOptions, MongoDriverInformation.builder().build() );
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeMongoDB( e );
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		mongo.close();
	}

	public MongoDatabase getDatabase() {
		return mongoDb;
	}

	private MongoDatabase extractDatabase(MongoClient mongo, MongoDBConfiguration config) {
		try {
			String databaseName = config.getDatabaseName();
			log.connectingToMongoDatabase( databaseName );

			Boolean containsDatabase = containsDatabase( mongo, databaseName );

			if ( containsDatabase == Boolean.FALSE ) {
				if ( config.isCreateDatabase() ) {
					log.creatingDatabase( databaseName );
				}
				else {
					throw log.databaseDoesNotExistException( config.getDatabaseName() );
				}
			}
			MongoDatabase db = mongo.getDatabase( databaseName );
			if ( containsDatabase == null ) {
				// force a connection to make sure we do have read access
				// otherwise the connection failure happens during the first flush
				MongoCursor<String> it = db.listCollectionNames().iterator();
				while ( it.hasNext() ) {
					if ( ( it.next().equalsIgnoreCase( "WeDoNotCareWhatItIsWeNeedToConnect" ) ) ) {
						containsDatabase = true;
						break;
					}
				}
			}
			return mongo.getDatabase( databaseName );
		}
		catch (MongoException me) {
			// The Mongo driver allows not to determine the cause of the error, eg failing authentication, anymore
			// by error code. At best the message contains some more information.
			// See also http://stackoverflow.com/questions/30455152/check-mongodb-authentication-with-java-3-0-driver
			throw log.unableToConnectToDatastore( me.getMessage(), me );
		}
	}

	private Boolean containsDatabase(MongoClient mongo, String databaseName) {
		try {
			for ( String existingName : mongo.listDatabaseNames() ) {
				if ( existingName.equals( databaseName ) ) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		catch (MongoException me) {
			// we don't have enough privileges, ignore the database creation
			return null;
		}
	}

	public void initializeBinaryStorageManager(OptionsService optionsService, Map<String, GridFSFields> map) {
		this.binaryStorageManager = new GridFSStorageManager( this, optionsService, map );
	}

	public GridFSStorageManager getBinaryStorageManager() {
		return binaryStorageManager;
	}
}
