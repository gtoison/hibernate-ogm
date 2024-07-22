/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jdbc.Work;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.query.impl.OgmQuerySqmImpl;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * An OGM specific session implementation which delegates most of the work to the underlying Hibernate ORM {@code Session},
 * except queries which are redirected to the OGM engine.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionImpl extends SessionDelegatorBaseImpl implements OgmSession, EventSource {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final OgmSessionFactoryImpl factory;

	public OgmSessionImpl(OgmSessionFactory factory, EventSource delegate) {
		super( delegate );
		this.factory = (OgmSessionFactoryImpl) factory;
	}

	//Overridden methods
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public OgmSessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Filter enableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public void disableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		throw new IllegalStateException( "Hibernate OGM does not support SQL Connections hence no Work" );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		checkOpen();
		return createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		checkOpen();
		return createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		return createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new OgmSharedSessionBuilderDelegator( delegate.sessionWithOptions(), factory );
	}

	public <G extends GlobalContext<?, ?>, D extends DatastoreConfiguration<G>> G configureDatastore(Class<D> datastoreType) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		delegate.removeOrphanBeforeUpdates( entityName, child );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}
	
	@Override
	public QueryImplementor createQuery(String queryString) {
		return createQuery( queryString, null );
	}
	
	@Override
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> expectedResultType) {
		checkOpen();
		// pulseTransactionCoordinator(); // TODO need to check what these do
		// delayedAfterCompletion();

		try {
			final HqlInterpretation<T> interpretation = interpretHql( queryString, expectedResultType );
			final QuerySqmImpl<T> query = new OgmQuerySqmImpl<>( queryString, interpretation, expectedResultType, this );
//			applyQuerySettingsAndHints( query ); TODO this is needed for locks and timeouts
			query.setComment( queryString );
			return query;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}
	
	protected <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		final QueryEngine queryEngine = getFactory().getQueryEngine();
		return queryEngine.getInterpretationCache()
				.resolveHqlInterpretation(
						hql,
						resultType,
						queryEngine.getHqlTranslator()
				);
	}
	
	@Override
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return super.createQuery( criteriaQuery );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		checkOpen();

		if ( Session.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SessionImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SharedSessionContractImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( OgmSession.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( EntityManager.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}

		throw new PersistenceException( "Hibernate OGM cannot unwrap " + clazz );
	}
}
