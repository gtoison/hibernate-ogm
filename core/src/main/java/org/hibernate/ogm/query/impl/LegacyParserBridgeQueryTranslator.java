/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

/**
 * A {@link QueryTranslator} which delegates most of the work to the existing JP-QL parser implementation. Specifically,
 * all methods which only depend on the structure of the incoming JP-QL query are delegated. Only those methods
 * depending on the translated query (such as
 * {@link #list(org.hibernate.engine.spi.SharedSessionContractImplementor, org.hibernate.engine.spi.QueryParameters)} are handled by
 * this class and its sub-classes. Over time, more and more methods should be implemented here rather than delegating
 * them.
 *
 * @author Gunnar Morling
 */
public abstract class LegacyParserBridgeQueryTranslator {
}
