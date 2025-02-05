/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.annotation.ElementType;
import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransactionException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.exception.EntityAlreadyExistsException;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.service.spi.ServiceException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import jakarta.persistence.PersistenceException;
import jakarta.transaction.SystemException;

/**
 * Log messages and exceptions used by Hibernate OGM core. Dialects may provide extensions of this interface with their
 * own specific log messages.
 * <p>
 * The following intervals for the message ids are to be used:
 * <ul>
 * <li>1-1000: core</li>
 * <li>1101-1200: infinispan</li>
 * <li>1201-1300: mongob</li>
 * <li>1301-1400: couchdb</li>
 * <li>1401-1500: neo4j</li>
 * <li>1501-1600: ehcache</li>
 * <li>1601-1700: redis</li>
 * <li>1701-1800: infinispan_remote</li>
 * </ul>
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @author Gunnar Morling
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends BasicLogger {

	@LogMessage(level = INFO)
	@Message(id = 1, value = "Hibernate OGM %1$s")
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(id = 2, value = "Could not find any META-INF/persistence.xml file in the classpath. " +
						"Unable to build Persistence Unit %1$s")
	void persistenceXmlNotFoundInClassPath(String unitName);

	@Message(id = 11, value = "Cannot instantiate GridDialect class [%1$s]")
	HibernateException cannotInstantiateGridDialect(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass, @Cause Exception e);

	@Message(id = 14, value = "%1$s has no constructor accepting org.hibernate.ogm.datastore.spi.DatastoreProvider" )
	HibernateException gridDialectHasNoProperConstructor(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass);

	@Message(id = 15, value = "Expected DatastoreProvider %2$s but found %1$s")
	HibernateException unexpectedDatastoreProvider(@FormatWith(ClassObjectFormatter.class) Class<?> found, @FormatWith(ClassObjectFormatter.class) Class<?> expected);

	@LogMessage(level = INFO)
	@Message(id = 16, value = "NoSQL Datastore provider: %1$s")
	void useDatastoreProvider(@FormatWith(ClassObjectFormatter.class) Class<?> datastoreProviderClass);

	@LogMessage(level = INFO)
	@Message(id = 17, value = "Grid Dialect: %1$s")
	void useGridDialect(@FormatWith(ClassObjectFormatter.class) Class<?> gridDialectClass);

	@Message(id = 18, value = "JTA transaction begin failed")
	TransactionException jtaTransactionBeginFailed(@Cause Exception e);

	@Message(id = 19, value = "JTA transaction commit failed")
	TransactionException jtaCommitFailed(@Cause Exception e);

	@Message(id = 20, value = "JTA transaction rollback failed")
	TransactionException jtaRollbackFailed(@Cause Exception e);

	@Message(id = 21, value = "Unable to mark JTA transaction for rollback")
	TransactionException unableToMarkTransactionForRollback(@Cause Exception e);

	@Message(id = 22, value = "Could not determine transaction status")
	TransactionException jtaCouldNotDetermineStatus(@Cause SystemException se);

	@Message(id = 23, value = "Unable to set transaction timeout to '%1$s'")
	TransactionException unableToSetTimeout(@Cause SystemException se, int timeout);

	@Message(id = 24, value = "Syntax error in query: [%1$s]")
	HibernateException querySyntaxException(@Cause Exception qse, String queryString);

	@LogMessage(level = ERROR)
	@Message(id = 25, value = "Batch indexing was interrupted")
	void interruptedBatchIndexing();

	@Message(id = 26, value = "Illegal discriminator type: '%1$s'")
	HibernateException illegalDiscrimantorType(String name);

	@Message(id = 27, value = "Could not convert string to discriminator object")
	HibernateException unableToConvertStringToDiscriminator(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 28, value = "Created query object '%2$s' from HQL/JP-QL query '%1$s'.")
	void createdQuery(String hqlQuery, Object queryObject);

	@LogMessage(level = WARN)
	@Message(id = 31, value = "OgmMassIndexer doesn't support the configuration option '%s'. Its setting will be ignored.")
	void unsupportedIndexerConfigurationOption(String optionName);

	@Message(id = 32, value = "Unable to support mapping subtypes that are not interfaces: %1$s")
	HibernateException mappingSubtypeNotInterface(@FormatWith(ClassObjectFormatter.class) Class<?> mappingType);

	@Message(id = 33, value = "Unable to create new proxy instance")
	HibernateException cannotCreateNewProxyInstance(@Cause Exception e);

	@Message(id = 34, value = "Annotation cannot be converted using %1$s")
	HibernateException cannotConvertAnnotation(@FormatWith(ClassObjectFormatter.class) Class<? extends AnnotationConverter<?>> converterClass, @Cause Exception e);

	@Message(id = 36, value = "Unable to load %1$s method from %2$s ")
	HibernateException unableToLoadContext(String methodName, @FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 37, value = "Unable to create global context proxy for type %1$s")
	HibernateException cannotCreateGlobalContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 38, value = "Unable to create entity context proxy for type %1$s")
	HibernateException cannotCreateEntityContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 39, value = "Unable to create property context proxy for type %1$s")
	HibernateException cannotCreatePropertyContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 41, value = "The given propery %1$s#%2$s with element type %3$s does not exist.")
	HibernateException getPropertyDoesNotExistException(String typeName, String property, ElementType elementType);

	@Message(id = 42, value = "The given element type %1$s is neither FIELD nor METHOD.")
	HibernateException getUnsupportedElementTypeException(ElementType elementType);

	@Message(id = 43, value = "Cannot instantiate type %1$s. Does it define a default constructor?")
	HibernateException unableToInstantiateType(@FormatWith(ClassObjectFormatter.class) Class<?> clazz, @Cause Exception e);

	@Message(id = 44, value = "Cannot load class %2$s specified via configuration property '%1$s'")
	HibernateException unableToLoadClass(String propertyName, String className, @Cause Exception e);

	@Message(id = 45, value = "Type %2$s specified via configuration property '%1$s' is not a sub-type of expected type %3$s")
	HibernateException unexpectedClassType(String propertyName, @FormatWith(ClassObjectFormatter.class) Class<?> clazz, @FormatWith(ClassObjectFormatter.class) Class<?> expectedClass);

	@Message(id = 46, value = "Object %2$s of type %3$s specified via configuration property '%1$s' is not of the expected type %4$s")
	HibernateException unexpectedInstanceType(String propertyName, String instance, @FormatWith(ClassObjectFormatter.class) Class<?> actualClass, @FormatWith(ClassObjectFormatter.class) Class<?> expectedClass);

	@Message(id = 47, value = "Either an option configurator may be specified via configuration property '%1$s' or OgmConfiguration#configureOptions() may be called, but not both at the same time.")
	HibernateException ambigiousOptionConfiguration(String propertyName);

	@Message(id = 48, value = "Unknown association storage strategy: [%s]. Supported values are: %s" )
	HibernateException unknownAssociationStorageStrategy(String databaseName, String supportedValues);

	@Message(id = 49, value = "The value set for the configuration property '" + OgmProperties.PORT + "' must be a number between 1 and 65535. Found '%s'.")
	HibernateException illegalPortValue(int value);

	@Message(id = 50, value = "The value set for the configuration property '%1$s' must be an integer number. Found '%2$s'.")
	HibernateException notAnInteger(String propertyName, String value);

	@Message(id = 51, value = "Unknown value given for configuration property '%1$s'; Found '%2$s', but supported values are: %3$s" )
	HibernateException unknownEnumerationValue(String propertyName, String value, String supportedValues);

	@Message(id = 52, value = "Missing value for property '%s'")
	HibernateException missingConfigurationProperty(String propertyName);

	@Message(id = 53, value = "Value of unsupported type given for configuration property '%1$s': '%2$s'")
	HibernateException unsupportedPropertyType(String propertyName, String value);

	@Message(id = 54, value = "It is not possible to add or poll operations from a closed queue")
	HibernateException closedOperationQueue();

	@Message(id = 55, value = "Invalid URL given for configuration property '%1$s': %2$s; The specified resource could not be found.")
	HibernateException invalidConfigurationUrl(String propertyName, String url);

	@Message(id = 56, value = "Unable to load record for retrieval of generated properties; Entity type: %1$s, id: %2$s")
	HibernateException couldNotRetrieveEntityForRetrievalOfGeneratedProperties(String entityType, Object id);

	@Message(id = 57, value = "'%s' must not be null")
	IllegalArgumentException mustNotBeNull(String name);

	@Message(id = 58, value = "Parameter '%s' must not be null")
	IllegalArgumentException parameterMustNotBeNull(String parameterName);

	@Message(id = 59, value = "Unable to find a GridType for %s")
	HibernateException unableToFindGridType(String typeName);

	@LogMessage(level = WARN)
	@Message(id = 60, value = "Sequence id generator used for entity '%2$s' is not supported by grid dialect %1$s, falling back to table-based id generation. Consider to use @TableGenerator rather than @SequenceGenerator.")
	void dialectDoesNotSupportSequences(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass, String entityName);

	@LogMessage(level = WARN)
	@Message(id = 61, value = "The option '@TableGenerator#catalog()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void catalogOptionNotSupportedForTableGenerator(String catalogName);

	@LogMessage(level = WARN)
	@Message(id = 62, value = "The option '@TableGenerator#schema()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void schemaOptionNotSupportedForTableGenerator(String schemaName);

	@LogMessage(level = WARN)
	@Message(id = 63, value = "The option '@SequenceGenerator#catalog()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void catalogOptionNotSupportedForSequenceGenerator(String catalogName);

	@LogMessage(level = WARN)
	@Message(id = 64, value = "The option '@SequenceGenerator#schema()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void schemaOptionNotSupportedForSequenceGenerator(String schemaName);

	@Message(id = 65, value = "Id generation strategy IDENTITY configured for entity %1$s is not supported by the current grid dialect.")
	HibernateException getIdentityGenerationStrategyNotSupportedException(String entityName);

	@LogMessage(level = WARN)
	@Message(id = 66, value = "Entity type %s uses an optimistic locking strategy which is not supported by the "
			+ "current grid dialect in an atomic manner. There will be two datastore round-trips for version checking and updating the data.")
	void usingNonAtomicOptimisticLocking(String entityName);

	@Message(id = 67, value = "Trying to insert an already existing entity: %s")
	EntityAlreadyExistsException mustNotInsertSameEntityTwice(String primaryKey, @Cause Exception taee );

	@Message(id = 68, value = "Could not configure property %1$s#%2$s")
	HibernateException couldNotConfigureProperty(String entityName, String string, @Cause Exception e);

	@Message(id = 69, value = "Grid dialect %1$s does not support lock mode %2$s")
	HibernateException unsupportedLockMode(@FormatWith(ClassObjectFormatter.class) Class<? extends GridDialect> dialectClass, LockMode lockMode);

	@LogMessage(level = WARN)
	@Message(id = 70, value = "'%1$s' is no valid datastore provider short name. Valid values are: %2$s")
	void noValidDatastoreProviderShortName(String providerName, String validProviderNames);

	@Message(id = 71, value = "Unable to start datastore provider")
	ServiceException unableToStartDatastoreProvider(@Cause Exception e);

	@Message(id = 72, value = "Unable to configure datastore provider")
	ServiceException unableToConfigureDatastoreProvider(@Cause Exception e);

	@Message(id = 73, value = "Couldn't load the Lucene-based query parser backend. Make sure the dependency "
			+ "org.hibernate.hql:hibernate-hql-lucene is part of the classpath.")
	HibernateException cannotLoadLuceneParserBackend(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 74, value = "Could not close session; swallowing exception as transaction completed")
	void unableToCloseSessionButSwallowingError( @Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 75, value = "JBoss Transaction Manager (com.arjuna.ats.jta.TransactionManager) detected on classpath. Using JBossStandAloneJtaPlatform as JTAPlatform implementation")
	void jbossTransactionManagerDetected();

	@LogMessage(level = INFO)
	@Message(id = 76, value = "No explicit or implicit defined JTAPlatform. Using NoJtaPlatform")
	void noJtaPlatformDetected();

	@Message(id = 77, value = "Parameter '%s' must not be an empty string")
	IllegalArgumentException parameterSringMustNotBeEmpty(String parameterName);

	@Message(id = 78, value = "Unrecognized alias in query:  %s")
	HibernateException getUnknownAliasException(String text);

	@Message(id = 79, value = "Unable to parse " + OgmProperties.HOST + " %s\n"
			+ "Property should be a comma separated list of host:port\n"
			+ "e.g. www.example.com, www2.example.com:123, 192.0.2.1, 192.0.2.2:123, 2001:db8::ff00:42:8329, [2001:db8::ff00:42:8329]:123")
	HibernateException unableToParseHost(String text);

	@Message(id = 80, value = "Could not load an entity batch: %s")
	HibernateException errorOnEntityBatchLoad(String contextualInfo, @Cause Exception e);

	@Message(id = 81, value = "The value set for the configuration property '%1$s' must be a long number. Found '%2$s'.")
	HibernateException notALong(String propertyName, String value);

	@Message(id = 82, value = "The entity at the inverse side of the association '%1$s' cannot be found in the session: %2$s")
	HibernateException entityTupleNotFound(String collectionRole, EntityKey entityKey);

	@Message(id = 83, value = "Failure when using JPA AttributeConverter [%1$s]. Is the datastore type of the converter a supported type for your datastore?")
	PersistenceException failureWhenUsingAttributeConverter(@FormatWith(ClassObjectFormatter.class) Class<?> converterClass, @Cause Exception e);

	@Message(id = 84, value = "Unable to find basic type support for [%2$s] when using JPA AttributeConverter [%1$s]." +
			"Is the datastore type of the converter a supported type for your datastore?")
	PersistenceException cannotFindTypeForAttributeConverter(@FormatWith(ClassObjectFormatter.class) Class<?> converted, @FormatWith(ClassObjectFormatter.class) Class<?> databaseColumnJavaType);

	@Message(id = 85, value = "Unable to find an entity entry for the entity '%1$s'")
	PersistenceException cannotFindEntityEntryForEntity(Object entity);

	@Message(id = 86, value = "Transaction identifier not available")
	HibernateException transactionIdIsNotAvailable();

	@Message(id = 87, value = "The tuple context is not available, probably because we are dealing with more than a single entity type")
	HibernateException tupleContextNotAvailable();

	@LogMessage(level = WARN)
	@Message(id = 88, value = "Configuration is referring to deprecated datastore provider name '%1$s'. Please use the new form '%2$s' instead.")
	void usingDeprecatedDatastoreProviderName(String deprecatedName, String newName);

	@Message(id = 89, value = "%1$s does not support queries on polymorphic entities using TABLE_PER_CLASS inheritance strategy. You should try using SINGLE_TABLE instead. Entities: %2$s")
	HibernateException queriesOnPolymorphicEntitiesAreNotSupportedWithTablePerClass( String datastore, Collection<String> subclassEntityNames );

	@Message(id = 90, value = "Returning multiple entities is not supported. Procedure '%1$s' expects results of type %2$s")
	HibernateException multipleEntitiesOutputNotSupported(String procedureName, Collection<?> synchronizedQuerySpaces);

	@Message(id = 91, value = "Projection and addEntity are not allowed in the same query on <%1$s> %2$s")
	HibernateException addEntityNotAllowedInNativeQueriesUsingProjection( String table, String query );

	@Message(id = 92, value = "Cannot execute stored procedure '%s'.")
	HibernateException cannotExecuteStoredProcedure(String storedProcedureName, @Cause Throwable e);

	@Message(id = 93, value = "Procedure with resolved name '%s' does not exist.")
	HibernateException procedureWithResolvedNameDoesNotExist(String procedureName, @Cause Exception e );

	@Message(id = 94, value = "Dialect %s does not support positional parameters when calling stored procedures")
	HibernateException dialectDoesNotSupportPositionalParametersForStoredProcedures( @FormatWith(ClassObjectFormatter.class) Class<?> dialectClass );

	@Message(id = 95, value = "Cannot set stored procedure '%s' parameter '%s' with value '%s'.")
	HibernateException cannotSetStoredProcedureParameter(String storedProcedureName, String param, Object value, @Cause Exception e);

	@Message(id = 96, value = "Cannot extract stored procedure '%s' result set '%s'.")
	HibernateException cannotExtractStoredProcedureResultSet(String storedProcedureName, Object resultSet, @Cause Exception e);

	@Message(id = 97, value = "Cannot extract tuple from value '%s'.")
	HibernateException cannotExtractTupleFromObject(Object value, @Cause Exception e);

	@Message(id = 98, value = "Error on fetch property " + OgmProperties.NATIVE_CLIENT_RESOURCE + ": '%s'.")
	HibernateException errorOnFetchJndiClientProperty(String nativeClientResource);

	@Message(id = 99, value = "Unable to parse order-by fragment: '%s'.")
	HibernateException unableToParseOrderByFragment(String queryFragment, @Cause Throwable e);

	@Message(id = 100, value = "Unable to render order-by fragment: '%s'.")
	HibernateException unableToRenderOrderByFragment(String queryFragment, @Cause Exception e);

	@Message(id = 101, value = "Error introspecting an object instance.")
	HibernateException errorIntrospectingObject(@Cause Exception e);
}
