/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.function.BiConsumer;

import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration;
import org.hibernate.ogm.id.impl.OgmIdentityGenerationStrategy;
import org.hibernate.ogm.id.impl.OgmSequenceGenerationStrategy;
import org.hibernate.ogm.id.impl.OgmTableGenerationStrategy;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.GenerationType;

/**
 * Register OGM strategies for identifier generations
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 * @author Guillaume Toison
 */
public class OgmMutableIdentifierGeneratorFactory implements GenerationTypeStrategyRegistration {

	@Override
	public void registerStrategies(BiConsumer<GenerationType, GenerationTypeStrategy> registry,	ServiceRegistry serviceRegistry) {
		registry.accept( GenerationType.TABLE, new OgmTableGenerationStrategy() );
		registry.accept( GenerationType.SEQUENCE, new OgmSequenceGenerationStrategy() );
		registry.accept( GenerationType.IDENTITY, new OgmIdentityGenerationStrategy() );
	}
}
