/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.proxy.EntityNotFoundDelegate;

/**
 * A {@link SessionFactoryBuilderImplementor} which creates {@link OgmSessionFactory} instances.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactoryBuilderImplementor extends SessionFactoryBuilderImplementor, OgmSessionFactoryBuilder {

	@Override
	OgmSessionFactoryBuilderImplementor applyValidatorFactory(Object validatorFactory);

	@Override
	OgmSessionFactoryBuilderImplementor applyBeanManager(Object beanManager);

	@Override
	OgmSessionFactoryBuilderImplementor applyName(String sessionFactoryName);

	@Override
	OgmSessionFactoryBuilderImplementor applyNameAsJndiName(boolean isJndiName);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutoClosing(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutoFlushing(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyStatisticsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyInterceptor(Interceptor interceptor);

	@Override
	OgmSessionFactoryBuilderImplementor addSessionFactoryObservers(SessionFactoryObserver... observers);

	@Override
	OgmSessionFactoryBuilderImplementor applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	@Override
	OgmSessionFactoryBuilderImplementor addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	@Override
	OgmSessionFactoryBuilderImplementor applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	@Override
	OgmSessionFactoryBuilderImplementor applyIdentifierRollbackSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyNullabilityChecking(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyLazyInitializationOutsideTransaction(boolean enabled);
	
	@Override
	OgmSessionFactoryBuilderImplementor applyBatchFetchStyle(BatchFetchStyle style);

	@Override
	OgmSessionFactoryBuilderImplementor applyDefaultBatchFetchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applyMaximumFetchDepth(int depth);

	@Override
	OgmSessionFactoryBuilderImplementor applyOrderingOfInserts(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyOrderingOfUpdates(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver);

	@Override
	@Deprecated
	OgmSessionFactoryBuilderImplementor applyJtaTrackingByThread(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyNamedQueryCheckingOnStartup(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applySecondLevelCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyQueryCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyCacheRegionPrefix(String prefix);

	@Override
	OgmSessionFactoryBuilderImplementor applyMinimalPutsForCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyStructuredCacheEntries(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyDirectReferenceCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutomaticEvictionOfCollectionCaches(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcBatchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcBatchingForVersionedEntities(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyScrollableResultsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyGetGeneratedKeysSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcFetchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applySqlComments(boolean enabled);

	@Override
	OgmSessionFactory build();
}
