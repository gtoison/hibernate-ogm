/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.id.impl;

import static org.hibernate.cfg.MappingSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A JPA sequence-based identifier generator (inspired by {@link SequenceStyleGenerator}.
 * <p>
 * This identifier generator is also used for JPA auto identifier generation. {@link OgmTableGenerator} is used as
 * fall-back, if the current datastore does not support sequences.
 * <table>
 * <caption>Configuration parameters</caption>
 * <tr>
 * <td><b>NAME</b></td>
 * <td><b>DESCRIPTION</b></td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#SEQUENCE_PARAM}</td>
 * <td>The name of the sequence to use store/retrieve sequence values</td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#INITIAL_PARAM}</td>
 * <td>The initial value of the sequence</td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#INCREMENT_PARAM}</td>
 * <td>The increment of the sequence</td>
 * </tr>
 * </table>
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Gunnar Morling
 */
public class OgmSequenceGenerator extends OgmGeneratorBase implements ExportableProducer {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private Type type;
	private Properties params;

	private QualifiedName logicalQualifiedSequenceName;
	private String sequenceName;
	private IdSourceKeyMetadata generatorKeyMetadata;

	private IdSourceKeyAndKeyMetadataProvider delegate;
	private SequenceStyleGenerator generator;

	public OgmSequenceGenerator() {
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		super.configure( type, params, serviceRegistry );
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		this.type = type;
		this.params = params;
		logicalQualifiedSequenceName = determineSequenceName( params, jdbcEnvironment, serviceRegistry );
		sequenceName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				logicalQualifiedSequenceName,
				jdbcEnvironment.getDialect()
		);
		generatorKeyMetadata = DefaultIdSourceKeyMetadata.forSequence( sequenceName );
		delegate = getDelegate( serviceRegistry );
		
		generator = new SequenceStyleGenerator();
		generator.configure(type, params, serviceRegistry);
	}

	@Override
	public IdSourceKeyMetadata getGeneratorKeyMetadata() {
		return delegate.getGeneratorKeyMetadata();
	}

	@Override
	protected IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
		return delegate.getGeneratorKey( session );
	}

	/**
	 * NOTE: Copied from SequenceStyleGenerator
	 *
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnv The JdbcEnvironment
	 * @return The sequence name
	 */
	protected QualifiedName determineSequenceName(
			Properties params,
			JdbcEnvironment jdbcEnv,
			ServiceRegistry serviceRegistry) {
		final Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
				getString( SequenceStyleGenerator.CATALOG, params )
		);
		final Identifier schema =  jdbcEnv.getIdentifierHelper().toIdentifier(
				getString( SequenceStyleGenerator.SCHEMA, params )
		);

		final String sequenceName = getString(
				SequenceStyleGenerator.SEQUENCE_PARAM,
				params,
				() -> getString( SequenceStyleGenerator.ALT_SEQUENCE_PARAM, params )
		);

		if ( StringHelper.isNotEmpty( sequenceName ) ) {
			// we have an explicit name, use it
			if ( sequenceName.contains( "." ) ) {
				return QualifiedNameParser.INSTANCE.parse( sequenceName );
			}
			else {
				return new QualifiedNameParser.NameParts(
						catalog,
						schema,
						jdbcEnv.getIdentifierHelper().toIdentifier( sequenceName )
				);
			}
		}

		// otherwise, determine an implicit name to use
		return determineImplicitName( catalog, schema, params, serviceRegistry );
	}
	
	/**
	 * NOTE: Copied from SequenceStyleGenerator
	 */
	private QualifiedName determineImplicitName(
			Identifier catalog,
			Identifier schema,
			Properties params,
			ServiceRegistry serviceRegistry) {
		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );

		final String namingStrategySetting = coalesceSuppliedValues(
				() -> {
					final String localSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
					if ( localSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return localSetting;
				},
				() -> {
					final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
					final String globalSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, configurationService.getSettings() );
					if ( globalSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return globalSetting;
				},
				StandardNamingStrategy.class::getName
		);

		final ImplicitDatabaseObjectNamingStrategy namingStrategy = strategySelector.resolveStrategy(
				ImplicitDatabaseObjectNamingStrategy.class,
				namingStrategySetting
		);

		return namingStrategy.determineSequenceName( catalog, schema, params, serviceRegistry );
	}

	private IdSourceKeyAndKeyMetadataProvider getDelegate(ServiceRegistry serviceRegistry) {
		GridDialect gridDialect = super.getGridDialect();

		if ( gridDialect.supportsSequences() ) {
			return new SequenceKeyAndMetadataProvider( generatorKeyMetadata );
		}
		else {
			log.dialectDoesNotSupportSequences(
					GridDialects.getWrappedDialect( gridDialect ),
					(String) params.get( SequenceStyleGenerator.JPA_ENTITY_NAME )
			);

			OgmTableGenerator tableGenerator = new OgmTableGenerator();
			Properties newParams = new Properties();
			newParams.putAll( params );
			newParams.put( OgmTableGenerator.SEGMENT_VALUE_PARAM, sequenceName );
			tableGenerator.configure( type, newParams, serviceRegistry );

			return new TableKeyAndMetadataProvider( tableGenerator );
		}
	}

	/**
	 * Provides a uniform way for handling the actual sequence case and the case of delegation to the table generator.
	 */
	private interface IdSourceKeyAndKeyMetadataProvider {
		IdSourceKeyMetadata getGeneratorKeyMetadata();
		IdSourceKey getGeneratorKey(SharedSessionContractImplementor session);
	}

	private static class TableKeyAndMetadataProvider implements IdSourceKeyAndKeyMetadataProvider {

		private final OgmTableGenerator delegate;

		public TableKeyAndMetadataProvider(OgmTableGenerator delegate) {
			this.delegate = delegate;
		}

		@Override
		public IdSourceKeyMetadata getGeneratorKeyMetadata() {
			return delegate.getGeneratorKeyMetadata();
		}

		@Override
		public IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
			return delegate.getGeneratorKey( session );
		}
	}

	private static class SequenceKeyAndMetadataProvider implements IdSourceKeyAndKeyMetadataProvider {

		private final IdSourceKey idSourceKey;

		private SequenceKeyAndMetadataProvider(IdSourceKeyMetadata idSourceKeyMetadata) {
			idSourceKey = IdSourceKey.forSequence( idSourceKeyMetadata );
		}

		@Override
		public IdSourceKeyMetadata getGeneratorKeyMetadata() {
			return idSourceKey.getMetadata();
		}

		@Override
		public IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
			return idSourceKey;
		}
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				logicalQualifiedSequenceName.getCatalogName(),
				logicalQualifiedSequenceName.getSchemaName()
		);
		Sequence sequence = namespace.locateSequence( logicalQualifiedSequenceName.getObjectName() );
		if ( sequence != null ) {
			sequence.validate( getInitialValue(), getIncrementSize() );
		}
		else {
			sequence = namespace.createSequence(
					logicalQualifiedSequenceName.getObjectName(),
					(physicalName) -> new Sequence(
							determineContributor( params ),
							namespace.getPhysicalName().getCatalog(),
							namespace.getPhysicalName().getSchema(),
							physicalName,
							getInitialValue(),
							getIncrementSize()
					)
			);
		}
	}
	
	private String determineContributor(Properties params) {
		final String contributor = params.getProperty( IdentifierGenerator.CONTRIBUTOR_NAME );

		return contributor == null ? "orm" : contributor;
	}
}
