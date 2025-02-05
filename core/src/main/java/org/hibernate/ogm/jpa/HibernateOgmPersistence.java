/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.jpa.impl.DelegatorPersistenceUnitInfo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

/**
 * JPA PersistenceProvider implementation specific to Hibernate OGM
 * All specific configurations are set transparently for the user.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateOgmPersistence implements PersistenceProvider {
	private static String IMPLEMENTATION_NAME = HibernateOgmPersistence.class.getName();

	private final HibernatePersistenceProvider delegate = new HibernatePersistenceProvider();

	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
		try {
			Map<?, ?> integration = map == null ? Collections.emptyMap() : Collections.unmodifiableMap( map );

			List<ParsedPersistenceXmlDescriptor> metadataFiles = PersistenceXmlParser.locatePersistenceUnits(
					integration
			);

			for ( ParsedPersistenceXmlDescriptor metadata : metadataFiles ) {
				//if the provider is not set, don't use it as people might want to use Hibernate ORM
				if ( IMPLEMENTATION_NAME.equalsIgnoreCase(
						metadata.getProviderClassName()
				) ) {
					//correct provider
					Map<Object, Object> protectiveCopy = new HashMap<Object, Object>( integration );
					enforceOgmConfig( protectiveCopy );
					protectiveCopy.put( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER, delegate.getClass().getName() );
					return delegate.createEntityManagerFactory(
							emName, protectiveCopy
					);
				}
			}

			//not the right provider
			return null;
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (Exception e) {
			throw new PersistenceException( "Unable to build EntityManagerFactory", e );
		}
	}

	private void enforceOgmConfig(Map<Object,Object> map) {
		//we use a placeholder DS to make sure, Hibernate EntityManager (Ejb3Configuration) does not enforce a different connection provider
		map.put( Environment.DATASOURCE, "---PlaceHolderDSForOGM---" );
		map.put( OgmProperties.ENABLED, true );
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
		final String persistenceProviderClassName = info.getPersistenceProviderClassName();
		if ( persistenceProviderClassName == null || IMPLEMENTATION_NAME.equals( persistenceProviderClassName ) ) {
			Map<Object, Object> protectiveCopy = map != null ? new HashMap<Object, Object>( map ) : new HashMap<Object, Object>();
			enforceOgmConfig( protectiveCopy );
			//HEM only builds an EntityManagerFactory when HibernatePersistence.class.getName() is the PersistenceProvider
			//that's why we override it when
			//new DelegatorPersistenceUnitInfo(info)
			return delegate.createContainerEntityManagerFactory(
					new DelegatorPersistenceUnitInfo(
							info
					),
					protectiveCopy
			);
		}
		//not the right provider
		return null;
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return delegate.getProviderUtil();
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		throw new IllegalStateException( "Hibernate OGM does not support schema generation" );
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		throw new IllegalStateException( "Hibernate OGM does not support schema generation" );
	}
}
