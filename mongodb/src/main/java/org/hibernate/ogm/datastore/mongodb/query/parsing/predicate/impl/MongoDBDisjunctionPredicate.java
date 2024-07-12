/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bson.Document;
import org.hibernate.ogm.query.spi.predicate.DisjunctionPredicate;
import org.hibernate.ogm.query.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.query.spi.predicate.Predicate;


/**
 * MongoDB-based implementation of {@link DisjunctionPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBDisjunctionPredicate extends DisjunctionPredicate<Document> implements NegatablePredicate<Document> {

	@Override
	public Document getQuery() {
		List<Document> elements = new ArrayList<Document>();

		for ( Predicate<Document> child : children ) {
			elements.add( child.getQuery() );
		}

		return new Document( "$or", elements );
	}

	@Override
	public Document getNegatedQuery() {
		List<Document> elements = new LinkedList<>();

		for ( Predicate<Document> child : children ) {
			elements.add( ( (NegatablePredicate<Document>) child ).getNegatedQuery() );
		}

		return new Document( "$and", elements );
	}
}
