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
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.NullPrecedence;

/**
 * A {@link SessionFactoryBuilder} which creates {@link OgmSessionFactory} instances.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactoryBuilder extends SessionFactoryBuilder {

	@Override
	OgmSessionFactoryBuilder applyValidatorFactory(Object validatorFactory);

	@Override
	OgmSessionFactoryBuilder applyBeanManager(Object beanManager);

	@Override
	OgmSessionFactoryBuilder applyName(String sessionFactoryName);

	@Override
	OgmSessionFactoryBuilder applyNameAsJndiName(boolean isJndiName);

	@Override
	OgmSessionFactoryBuilder applyAutoClosing(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyAutoFlushing(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyStatisticsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyInterceptor(Interceptor interceptor);

	@Override
	OgmSessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers);

	@Override
	OgmSessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	@Override
	OgmSessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	@Override
	OgmSessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	@Override
	OgmSessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyNullabilityChecking(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyBatchFetchStyle(BatchFetchStyle style);

	@Override
	OgmSessionFactoryBuilder applyDefaultBatchFetchSize(int size);

	@Override
	OgmSessionFactoryBuilder applyMaximumFetchDepth(int depth);

	@Override
	OgmSessionFactoryBuilder applyDefaultNullPrecedence(NullPrecedence nullPrecedence);

	@Override
	OgmSessionFactoryBuilder applyOrderingOfInserts(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyOrderingOfUpdates(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver);

	@Override
	@Deprecated
	OgmSessionFactoryBuilder applyJtaTrackingByThread(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyQueryCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyCacheRegionPrefix(String prefix);

	@Override
	OgmSessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyStructuredCacheEntries(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyDirectReferenceCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyJdbcBatchSize(int size);

	@Override
	OgmSessionFactoryBuilder applyJdbcBatchingForVersionedEntities(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyScrollableResultsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilder applyJdbcFetchSize(int size);

	@Override
	OgmSessionFactoryBuilder applySqlComments(boolean enabled);

	@Override
	OgmSessionFactory build();
}
