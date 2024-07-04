/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

public class NoSQLProcedureCallMemento implements NamedCallableQueryMemento {

	private final NamedCallableQueryMemento delegate;

	public NoSQLProcedureCallMemento(NamedCallableQueryMemento delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public <T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> javaType) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session, Class<?>... resultSetJavaTypes) {
		return new NoSQLProcedureCallImpl( session, delegate.getCallableName(), resultSetJavaTypes );
	}
	
	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session, String... resultSetMappingNames) {
		return new NoSQLProcedureCallImpl( session, delegate.getCallableName(), resultSetMappingNames );
	}
	
	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new NoSQLProcedureCallImpl( session, this );
	}
	
	@Override
	public Map<String, Object> getHints() {
		return delegate.getHints();
	}
	
	@Override
	public String getComment() {
		return delegate.getComment();
	}
	
	@Override
	public String getCacheRegion() {
		return delegate.getCacheRegion();
	}
	
	@Override
	public void validate(QueryEngine queryEngine) {
		delegate.validate( queryEngine );
	}
	
	@Override
	public FlushMode getFlushMode() {
		return delegate.getFlushMode();
	}
	
	@Override
	public Integer getFetchSize() {
		return delegate.getFetchSize();
	}
	
	@Override
	public Integer getTimeout() {
		return delegate.getTimeout();
	}
	
	@Override
	public Set<String> getQuerySpaces() {
		return delegate.getQuerySpaces();
	}
	
	@Override
	public NamedNativeQueryMemento makeCopy(String name) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}
	
	@Override
	public Boolean getCacheable() {
		return delegate.getCacheable();
	}
	
	@Override
	public Boolean getReadOnly() {
		return delegate.getReadOnly();
	}

	@Override
	public String getRegistrationName() {
		return delegate.getRegistrationName();
	}
	
	@Override
	public List<ParameterMemento> getParameterMementos() {
		return delegate.getParameterMementos();
	}
	
	@Override
	public String[] getResultSetMappingNames() {
		return delegate.getResultSetMappingNames();
	}
	
	@Override
	public Class<?>[] getResultSetMappingClasses() {
		return delegate.getResultSetMappingClasses();
	}
	
	@Override
	public String getCallableName() {
		return delegate.getCallableName();
	}
	
	@Override
	public ParameterStrategy getParameterStrategy() {
		return delegate.getParameterStrategy();
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( delegate ) ) {
			return (T) delegate;
		}
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}
		throw new HibernateException( "Cannot unwrap the following type: " + cls );
	}
}
