/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.nativeloader.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.param.ParameterBinder;
import org.hibernate.query.NativeQuery.RootReturn;

/**
 * Extension point allowing any NoSQL native query with named and positional parameters
 * to be executed by OGM on the corresponding backend, returning managed entities, collections and
 * simple scalar values.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BackendCustomQuery<T extends Serializable> implements CustomQuery, Serializable {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final String queryString;
	private final T queryObject;
	private final Set<String> querySpaces;
	private final List<Return> customQueryReturns;

	private final EntityMetadataInformation singleEntityMetadataInformation;

	public BackendCustomQuery(String queryString, T query, NativeSQLQueryReturn[] queryReturns, Set<String> querySpaces, SessionFactoryImplementor factory) throws HibernateException {
		LOG.tracev( "Starting processing of NoSQL query [{0}]", queryString );

		this.queryString = queryString;
		this.queryObject = query;

		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor( queryReturns, factory );
		processor.process();
		customQueryReturns = Collections.unmodifiableList( processor.generateCustomReturns( false ) );

		if ( querySpaces != null ) {
			this.querySpaces = Collections.<String>unmodifiableSet( querySpaces );
		}
		else {
			this.querySpaces = Collections.emptySet();
		}

		this.singleEntityMetadataInformation = determineSingleEntityMetadataInformation( factory, customQueryReturns );
	}

	private EntityMetadataInformation determineSingleEntityMetadataInformation(SessionFactoryImplementor sessionFactory, List<Return> customQueryReturns) {
		EntityMetadataInformation metadataInformation = null;

		for ( Return queryReturn : customQueryReturns ) {
			if ( queryReturn instanceof RootReturn ) {
				if ( metadataInformation != null ) {
					return null;
				}
				RootReturn rootReturn = (RootReturn) queryReturn;
				OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( rootReturn.getEntityName() );
				metadataInformation = new EntityMetadataInformation( persister.getEntityKeyMetadata(), rootReturn.getEntityName()
				);
			}
		}

		return metadataInformation;
	}

	/**
	 * @deprecated Use {@link #getQueryString()} instead.
	 */
	@Override
	@Deprecated
	public String getSQL() {
		return getQueryString();
	}

	public String getQueryString() {
		return queryString;
	}

	/**
	 * Returns an object-based representation of this query, if present.
	 *
	 * @return an object-based representation of this query, or {@code null} if this is a string-based query.
	 */
	public T getQueryObject() {
		return queryObject;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public List<ParameterBinder> getParameterValueBinders() {
		return Collections.emptyList();
	}

	@Override
	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	/**
	 * Returns the {@link EntityMetadataInformation} of the entity type selected by this query.
	 *
	 * @return the {@link EntityMetadataInformation} of the entity type selected by this query or {@code null} in case this
	 * query does not select exactly one entity type (e.g. in case of scalar values or joins (if supported in future revisions)).
	 */
	public EntityMetadataInformation getSingleEntityMetadataInformationOrNull() {
		return singleEntityMetadataInformation;
	}

	@Override
	public String toString() {
		return queryObject != null ? queryObject.toString() : queryString;
	}
}
