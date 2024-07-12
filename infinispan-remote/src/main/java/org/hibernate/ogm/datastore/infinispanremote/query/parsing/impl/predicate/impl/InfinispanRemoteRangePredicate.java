/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;
import org.hibernate.ogm.query.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.query.spi.predicate.RangePredicate;

/**
 * Infinispan remote-based implementation of {@link RangePredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteRangePredicate extends RangePredicate<InfinispanRemoteQueryBuilder> implements NegatablePredicate<InfinispanRemoteQueryBuilder> {

	public InfinispanRemoteRangePredicate(String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
	}

	@Override
	public InfinispanRemoteQueryBuilder getQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder();
		builder.append( "( " );
		builder.append( propertyName );
		builder.append( " >= " );

		builder.appendValue( lower );

		builder.append( " && " );
		builder.append( propertyName );
		builder.append( " <= " );

		builder.appendValue( upper );

		builder.append( " )" );
		return builder;
	}

	@Override
	public InfinispanRemoteQueryBuilder getNegatedQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder();
		builder.append( "( " );
		builder.append( propertyName );
		builder.append( " < " );

		builder.appendValue( lower );

		builder.append( " || " );
		builder.append( propertyName );
		builder.append( " > " );

		builder.appendValue( upper );

		builder.append( " )" );
		return builder;
	}
}
