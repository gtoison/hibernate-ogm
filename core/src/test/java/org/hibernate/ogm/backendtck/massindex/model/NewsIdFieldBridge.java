/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex.model;

import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * This is a simple implementation used by some tests; it's not supposed to be an example on how to implement a
 * {@link ValueBridge}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NewsIdFieldBridge implements ValueBridge<NewsID, String> {

	private static final String SEP = "::::";

	@Override
	public String toIndexedValue(NewsID newsId, ValueBridgeToIndexedValueContext context) {
		return newsId == null ? null : newsId.getTitle() + SEP + newsId.getAuthor();
	}

	@Override
	public NewsID fromIndexedValue(String stringValue, ValueBridgeFromIndexedValueContext context) {
		String[] split = stringValue.split( SEP );
		return new NewsID( split[0], split[1] );
	}
}
