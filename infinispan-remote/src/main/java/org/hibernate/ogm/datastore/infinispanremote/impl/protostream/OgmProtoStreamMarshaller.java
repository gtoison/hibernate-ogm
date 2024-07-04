/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.io.IOException;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.multimessage.MultiMessage;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.multimessage.MultiMessageExtension;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.ImmutableProtoStreamMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * The original ProtoStreamMarshaller expects 1:1 mapping between a Java class
 * and a protostream schema.
 * <p>
 * We need flexibility to override this by choosing {@link org.infinispan.protostream.impl.BaseMarshallerDelegate}
 * using the Protocol Buffer type even in marshalling phase.
 *
 * @author Fabio Massimo Ercoli
 * @see MultiMessageExtension
 * @see MultiMessage
 */
public final class OgmProtoStreamMarshaller extends ImmutableProtoStreamMarshaller {

	public OgmProtoStreamMarshaller( ImmutableSerializationContext serializationContext ) {
		super( serializationContext );
	}

	@Override
	protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException {
		if ( !( o instanceof MultiMessage ) ) {
			return super.objectToBuffer( o, estimatedSize );
		}

		byte[] bytes = MultiMessageExtension.toWrappedByteArray( getSerializationContext(), (MultiMessage) o );
		return ByteBufferImpl.create( bytes, 0, bytes.length );
	}
}
