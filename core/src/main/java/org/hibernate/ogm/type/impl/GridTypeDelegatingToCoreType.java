/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * Delegates non persistence operations to the underlying Hibernate Core Type.
 *
 * @author Emmanuel Bernard
 */
public abstract class GridTypeDelegatingToCoreType implements GridType {
	private final Type delegate;

	GridTypeDelegatingToCoreType(Type type) {
		this.delegate = type;
	}

	@Override
	public boolean isAssociationType() {
		return delegate.isAssociationType();
	}

	@Override
	public boolean isCollectionType() {
		return delegate.isCollectionType();
	}

	@Override
	public boolean isEntityType() {
		return delegate.isEntityType();
	}

	@Override
	public boolean isAnyType() {
		return delegate.isAnyType();
	}

	@Override
	public boolean isComponentType() {
		return delegate.isComponentType();
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return delegate.getColumnSpan( mapping );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class getReturnedClass() {
		return delegate.getReturnedClass();
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		return delegate.isSame( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return delegate.isEqual( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.isEqual( x, y, factory );
	}

	@Override
	public int getHashCode(Object x) throws HibernateException {
		return delegate.getHashCode( x );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.getHashCode( x, factory );
	}

	@Override
	public int compare(Object x, Object y) {
		return delegate.compare( x, y );
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) throws HibernateException {
		return delegate.isDirty( old, current, session );
	}

	@Override
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return delegate.isDirty( oldState, currentState, checkable, session );
	}

	@Override
	public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return delegate.isModified( dbState, currentState, checkable, session );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.toLoggableString( value, factory );
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.deepCopy( value, factory );
	}

	@Override
	public boolean isMutable() {
		return delegate.isMutable();
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return delegate.disassemble( value, session, owner );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return delegate.assemble( cached, session, owner );
	}

	@Override
	public void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
		delegate.beforeAssemble( cached, session );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache, ForeignKeyDirection foreignKeyDirection)
			throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache, foreignKeyDirection );
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return delegate.toColumnNullness( value, mapping );
	}
}
