/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import java.util.Set;

import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.infinispan.Cache;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class InfinispanTupleSnapshot implements TupleSnapshot {
	private final Cache<String, Object> atomicMap;

	public InfinispanTupleSnapshot(Cache<String,Object> atomicMap) {
		this.atomicMap = atomicMap;
	}
	@Override
	public Object get(String column) {
		return atomicMap.get( column );
	}

	@Override
	public boolean isEmpty() {
		return atomicMap.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return atomicMap.keySet();
	}

	public Cache<String, Object> getAtomicMap() {
		return atomicMap;
	}
}
