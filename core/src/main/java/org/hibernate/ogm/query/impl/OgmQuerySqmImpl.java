/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;

/**
 * 
 * @author Guillaume Toison
 */
public class OgmQuerySqmImpl<R> extends QuerySqmImpl<R> {

	public OgmQuerySqmImpl(String hql, HqlInterpretation<R> hqlInterpretation, Class<R> resultType,
			SharedSessionContractImplementor session) {
		super( hql, hqlInterpretation, resultType, session );
	}

	@Override
	protected <T> ConcreteSqmSelectQueryPlan<T> buildConcreteQueryPlan(SqmSelectStatement<T> concreteSqmStatement,
			Class<T> expectedResultType, TupleMetadata tupleMetadata, QueryOptions queryOptions) {
		return new OgmConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				getDomainParameterXref(),
				expectedResultType,
				tupleMetadata,
				queryOptions
		);
	}
}
