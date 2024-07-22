/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.List;

import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.internal.PluralAttributeMappingImpl;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.impl.RowKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.AssociationOrderBy;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.DeprecatedCollectionStuff;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;

/**
 * CollectionPersister storing the collection in a grid
 *
 * @author Emmanuel Bernard
 */
public interface OgmCollectionPersister extends CollectionPhysicalModel, CollectionPersister, CollectionMutationTarget, PluralAttributeMappingImpl.Aware, FetchProfileAffectee, DeprecatedCollectionStuff {

	default AssociationKeyMetadata buildAssociationKeyMetadata(final Collection collection) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		String[] rowKeyColumnNames = rowKeyBuilder.getColumnNames();
		String[] rowKeyIndexColumnNames = rowKeyBuilder.getIndexColumnNames();

		List<AssociationOrderBy> manyToManyOrderBy = null;
		if ( collection.getManyToManyOrdering() != null ) {
			// TODO implement this again
//			manyToManyOrderBy = OGMOrderByParser.parse( collection.getManyToManyOrdering(), persisterCreationContext.getSessionFactory(), new BasicColumnMapperImpl() );
		}
		
		return new DefaultAssociationKeyMetadata.Builder()
				.table( getTableName() )
				.columnNames( getKeyColumnNames() )
				.rowKeyColumnNames( rowKeyColumnNames )
				.rowKeyIndexColumnNames( rowKeyIndexColumnNames )
				.manyToManyOrderBy( manyToManyOrderBy )
				.entityKeyMetadata( ( (OgmEntityPersister) getOwnerEntityPersister() ).getEntityKeyMetadata() )
				.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( getElementColumnNames(), targetEntityKeyMetadata( false ) ) )
				.inverse( collection.isInverse() )
				.collectionRole( getUnqualifiedRole() )
				.associationKind( getElementType().isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION )
				.associationType( getAssociationType( collection ) )
				.build();
	}

	private static org.hibernate.ogm.model.key.spi.AssociationType getAssociationType(Collection collection) {
		CollectionType collectionType = collection.getCollectionType();

		if ( collectionType.isArrayType() ) {
			return org.hibernate.ogm.model.key.spi.AssociationType.LIST;
		}
		else if ( collectionType instanceof SetType ) {
			return org.hibernate.ogm.model.key.spi.AssociationType.SET;
		}
		else if ( collectionType instanceof MapType ) {
			return org.hibernate.ogm.model.key.spi.AssociationType.MAP;
		}
		else if ( collectionType instanceof ListType ) {
			return org.hibernate.ogm.model.key.spi.AssociationType.LIST;
		}
		else {
			return org.hibernate.ogm.model.key.spi.AssociationType.BAG;
		}
	}

	default String getUnqualifiedRole() {
		String entity = getOwnerEntityPersister().getEntityName();
		String role = getRole();
		return role.substring( entity.length() + 1 );
	}

	private EntityKeyMetadata targetEntityKeyMetadata( boolean inverse ) {
		if ( inverse ) {
			// Bidirectional *ToMany
			return ( (OgmEntityPersister) getOwnerEntityPersister() ).getEntityKeyMetadata();
		}
		else if ( getElementType().isEntityType() ) {
			// *ToMany
			return ( (OgmEntityPersister) getElementPersister() ).getEntityKeyMetadata();
		}
		else {
			// Embedded we need to build the key metadata
			String[] targetColumnNames = null;
			if ( inverse ) {
				targetColumnNames = getKeyColumnNames();
			}
			else {
				targetColumnNames = getElementColumnNames();
			}
			return new DefaultEntityKeyMetadata( getTableName(), targetColumnNames );
		}
	}

	AssociationKeyMetadata getAssociationKeyMetadata();

	// Centralize the RowKey column setting logic as the values settings are slightly different between insert / update and delete
	default RowKeyBuilder initializeRowKeyBuilder() {
		RowKeyBuilder builder = new RowKeyBuilder();
		if ( hasIdentifier() ) {
			builder.addColumns( getIdentifierColumnName() );
		}
		else {
			builder.addColumns( getKeyColumnNames() );
			// !isOneToMany() present in delete not in update
			if ( !isOneToMany() && hasPhysicalIndexColumn() ) {
				builder.addIndexColumns( getIndexColumnNames() );
			}
			else {
				builder.addColumns( getElementColumnNames() );
			}
		}
		return builder;
	}

	/**
	 * Get the property name representing the collection on the main side of the association.
	 *
	 * @return the property name on the main side, if this collection represents the inverse (non-main) side of a
	 * bi-directional association, this association's own property name otherwise.
	 */
	String getMainSidePropertyName();

	/**
	 * Returns the association type context providing meta-data to be passed to grid dialects when working on this
	 * association.
	 * <p>
	 * <b>Note:</b> Due to initialization order related constraints, this method may only be invoked after all
	 * collection and entity persisters have been set up. Use {@link #getAssociationTypeContext(String)} when in need of
	 * a context prior to that point.
	 *
	 * @return the association type context, it never returns {@code null}
	 */
	AssociationTypeContext getAssociationTypeContext();

	default AssociationTypeContext getAssociationTypeContext(String mainSidePropertyName) {
		OptionsServiceContext serviceContext = getFactory()
				.getServiceRegistry()
				.getService( OptionsService.class )
				.context();

		AssociationTypeContext associationTypeContext = new AssociationTypeContextImpl.Builder( serviceContext )
				.associationKeyMetadata( getAssociationKeyMetadata() )
				.hostingEntityPersister( getOwnerEntityPersister() )
				.mainSidePropertyName( mainSidePropertyName )
				.build();

		return associationTypeContext;
	}
	
	OgmEntityPersister getOwnerEntityPersister();

	default AssociationPersister getAssociationPersister(Object collectionOwner, Object id, SharedSessionContractImplementor session) {
		SessionFactoryImplementor factory = session.getSessionFactory();
		ServiceRegistry serviceRegistry = factory.getServiceRegistry();
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		
		return new AssociationPersister.Builder(
				getOwnerEntityPersister().getMappedClass()
			)
			.hostingEntity( collectionOwner )
			.gridDialect( gridDialect )
			.key( id, getAttributeMapping().getKeyDescriptor() )
			.associationKeyMetadata( getAssociationKeyMetadata() )
			.associationTypeContext( getAssociationTypeContext() )
			.session( session )
			.build();
	}
	
	default boolean hasIdentifier() {
		return getCollectionSemantics().getCollectionClassification() == CollectionClassification.ID_BAG;
	}
}
