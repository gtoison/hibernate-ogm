/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class GetterPersistenceUnitInfo implements PersistenceUnitInfo {
	private String persistenceUnitName;
	private String persistenceProviderClassName;
	private PersistenceUnitTransactionType transactionType;
	private DataSource jtaDataSource;
	private DataSource nonJtaDataSource;
	private List<String> mappingFileNames = new ArrayList<String>( );
	private List<URL> jarFileUrls = new ArrayList<URL>(  );
	private URL persistenceUnitRootUrl;
	private List<String> managedClassNames = new ArrayList<String>(  );
	private boolean excludeUnlistedClasses;
	private SharedCacheMode sharedCacheMode;
	private ValidationMode validationMode;
	private Properties properties;
	private String persistenceXMLSchemaVersion;
	private ClassLoader classLoader;

	@Override
	public void addTransformer(ClassTransformer transformer) {
		//nothing to do
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}

	@Override
	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	public String getPersistenceProviderClassName() {
		return persistenceProviderClassName;
	}

	public void setPersistenceProviderClassName(String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public DataSource getJtaDataSource() {
		return jtaDataSource;
	}

	public void setJtaDataSource(DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public void setNonJtaDataSource(DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFileNames;
	}

	public void setMappingFileNames(List<String> mappingFileNames) {
		this.mappingFileNames = mappingFileNames;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return jarFileUrls;
	}

	public void setJarFileUrls(List<URL> jarFileUrls) {
		this.jarFileUrls = jarFileUrls;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	public void setManagedClassNames(List<String> managedClassNames) {
		this.managedClassNames = managedClassNames;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return persistenceXMLSchemaVersion;
	}

	public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "GetterPersistenceUnitInfo" );
		sb.append( "{persistenceUnitName='" ).append( persistenceUnitName ).append( '\'' );
		sb.append( ", persistenceProviderClassName='" ).append( persistenceProviderClassName ).append( '\'' );
		sb.append( ", transactionType=" ).append( transactionType );
		sb.append( ", jtaDataSource=" ).append( jtaDataSource );
		sb.append( ", nonJtaDataSource=" ).append( nonJtaDataSource );
		sb.append( ", mappingFileNames=" ).append( mappingFileNames );
		sb.append( ", jarFileUrls=" ).append( jarFileUrls );
		sb.append( ", persistenceUnitRootUrl=" ).append( persistenceUnitRootUrl );
		sb.append( ", managedClassNames=" ).append( managedClassNames );
		sb.append( ", excludeUnlistedClasses=" ).append( excludeUnlistedClasses );
		sb.append( ", sharedCacheMode=" ).append( sharedCacheMode );
		sb.append( ", validationMode=" ).append( validationMode );
		sb.append( ", properties=" ).append( properties );
		sb.append( ", persistenceXMLSchemaVersion='" ).append( persistenceXMLSchemaVersion ).append( '\'' );
		sb.append( ", classLoader=" ).append( classLoader );
		sb.append( '}' );
		return sb.toString();
	}
}
