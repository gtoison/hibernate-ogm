/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Emmanuel Bernard
 */
public class CollectionType extends GridTypeDelegatingToCoreType {
	private final org.hibernate.type.CollectionType delegate;

	public CollectionType(org.hibernate.type.CollectionType type) {
		super( type );
		this.delegate = type;
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return resolve( null, session, owner );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException {
		//NOOP
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SharedSessionContractImplementor session)
			throws HibernateException {
		//NOOP
	}

	@Override
	public Object convertToBackendType(Object value, SessionFactoryImplementor sessionFactory) {
		return null;
	}
}
