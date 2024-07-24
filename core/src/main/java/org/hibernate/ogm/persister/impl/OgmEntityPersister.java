/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.internal.InFlightEntityMappingType;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.TransactionContextHelper;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.DeprecatedEntityStuff;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
/**
 * Basic functionality for persisting an entity using OGM.
 * Implements the same interfaces as {@link AbstractEntityPersister}
 * 
 * @author Emmanuel Bernard
 * @author Davide D'Alto
 * @author Guillaume Toison
 */
public interface OgmEntityPersister extends InFlightEntityMappingType, EntityMutationTarget, LazyPropertyInitializer, PostInsertIdentityPersister, FetchProfileAffectee, DeprecatedEntityStuff {
	EntityKeyMetadata getEntityKeyMetadata();
	
	default EntityKeyMetadata getRootEntityKeyMetadata() {
		//we only support single table and table per concrete class strategies
		//in this case the root to lock to is the entity itself
		//see its use in read locking strategy.
		return getEntityKeyMetadata();
	}
	
	default TupleTypeContextImpl createTupleTypeContext(ServiceRegistry serviceRegistry) {
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		
		Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata = newHashMap();
		Map<String, String> roles = newHashMap();

		for ( int index = 0; index < getPropertySpan(); index++ ) {
			final Type uniqueKeyType = getPropertyTypes()[index];
			if ( uniqueKeyType.isEntityType() ) {
				OgmEntityPersister associatedJoinable = (OgmEntityPersister) getFactory().getMetamodel().entityPersister(
						( (EntityType) uniqueKeyType ).getAssociatedEntityName() );

				for ( String column : getPropertyColumnNames( index ) ) {
					associatedEntityKeyMetadata.put( column, new DefaultAssociatedEntityKeyMetadata( getPropertyColumnNames( index ), associatedJoinable.getEntityKeyMetadata() ) );
					roles.put( column, getPropertyNames()[index] );
				}
			}
		}

		List<String> selectableColumnNames = selectableColumnNames( this, getDiscriminatorColumnName(), gridDialect );
		Set<String> polymorphicEntityColumns = polymorphicEntityColumns( this, selectableColumnNames, gridDialect );
		return new TupleTypeContextImpl(
				selectableColumnNames,
				polymorphicEntityColumns,
				associatedEntityKeyMetadata,
				roles,
				optionsService.context().getEntityOptions( getMappedClass() ),
				getDiscriminatorColumnName(),
				getDiscriminatorValue()
		);
	}
	
	default Map<String, AssociationKeyMetadata> initInverseOneToOneAssociationKeyMetadata() {
		Map<String, AssociationKeyMetadata> associationKeyMetadata = new HashMap<String, AssociationKeyMetadata>();
		for ( String property : getPropertyNames() ) {
			Type propertyType = getPropertyType( property );

			if ( !propertyType.isEntityType() ) {
				continue;
			}

			String[] propertyColumnNames = getPropertyColumnNames( getPropertyIndex( property ) );
			String[] rowKeyColumnNames = buildRowKeyColumnNamesForStarToOne( this, propertyColumnNames );

			OgmEntityPersister otherSidePersister = (OgmEntityPersister) ( (EntityType) propertyType ).getAssociatedJoinable( getFactory() );
			String inverseOneToOneProperty = getInverseOneToOneProperty( property, otherSidePersister );

			if ( inverseOneToOneProperty != null ) {
				EntityKeyMetadata entityKeyMetadata = getEntityKeyMetadata();
				AssociationKeyMetadata metadata = new DefaultAssociationKeyMetadata.Builder()
						.table( getTableName() )
						.columnNames( propertyColumnNames )
						.rowKeyColumnNames( rowKeyColumnNames )
						.entityKeyMetadata( otherSidePersister.getEntityKeyMetadata() )
						.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( entityKeyMetadata.getColumnNames(), entityKeyMetadata ) )
						.inverse( true )
						.collectionRole( inverseOneToOneProperty )
						.associationKind( AssociationKind.ASSOCIATION )
						.associationType( org.hibernate.ogm.model.key.spi.AssociationType.ONE_TO_ONE )
						.build();

				associationKeyMetadata.put( property, metadata );
			}
		}

		return associationKeyMetadata;
	}

	/**
	 * Returns the name from the inverse side if the given property de-notes a one-to-one association.
	 */
	private String getInverseOneToOneProperty(String property, OgmEntityPersister otherSidePersister) {
		for ( String candidate : otherSidePersister.getPropertyNames() ) {
			Type candidateType = otherSidePersister.getPropertyType( candidate );
			if ( candidateType.isEntityType()
					&& ( ( (EntityType) candidateType ).isOneToOne()
					&& isOneToOneMatching( this, property, (OneToOneType) candidateType ) ) ) {
				return candidate;
			}
		}

		return null;
	}

	private static boolean isOneToOneMatching(OgmEntityPersister mainSidePersister, String mainSideProperty, OneToOneType inversePropertyType) {
		SessionFactoryImplementor factory = mainSidePersister.getFactory();
		String associatedProperty = inversePropertyType.getRHSUniqueKeyPropertyName();

		// If that's a OneToOne check the associated property name and see if it matches where we come from
		return mainSidePersister == inversePropertyType.getAssociatedJoinable( factory ) && mainSideProperty.equals( associatedProperty );
	}

	
	private static List<String> selectableColumnNames(final OgmEntityPersister persister, final String discriminatorColumnName, GridDialect gridDialect) {
		Set<String> columnNames = new HashSet<String>();

		for ( int propertyCount = 0; propertyCount < persister.getPropertySpan(); propertyCount++ ) {
			String[] property = persister.getPropertyColumnNames( propertyCount );
			for ( int columnCount = 0; columnCount < property.length; columnCount++ ) {
				columnNames.add( property[columnCount] );
			}
		}

		if ( discriminatorColumnName != null ) {
			columnNames.add( discriminatorColumnName );
		}

		columnNames.addAll( persister.getEmbeddedCollectionColumns( gridDialect ) );

		return new ArrayList<>( columnNames );
	}

	private static Set<String> polymorphicEntityColumns(final OgmEntityPersister persister, List<String> selectableColumnNames, GridDialect gridDialect) {
		Set<String> columnNames = new HashSet<>();
		if ( !persister.getEntityMetamodel().getSubclassEntityNames().isEmpty() ) {
			@SuppressWarnings("unchecked")
			Set<String> subclasses = persister.getEntityMetamodel().getSubclassEntityNames();
			for ( String className : subclasses ) {
				OgmEntityPersister subEntityPersister = (OgmEntityPersister) persister.getFactory().getMetamodel().entityPersister( className );
				if ( !subEntityPersister.equals( persister ) ) {
					List<String> subEntityColumnNames = selectableColumnNames( subEntityPersister, null, gridDialect );
					for ( String column : subEntityColumnNames ) {
						if ( !selectableColumnNames.contains( column ) ) {
							columnNames.add( column );
						}
					}
				}
			}
		}
		return columnNames;
	}

	/**
	 * Returns the names of all those columns which represent a collection to be stored within the owning entity
	 * structure (element collections and/or *-to-many associations, depending on the dialect's capabilities).
	 */
	private List<String> getEmbeddedCollectionColumns(GridDialect gridDialect) {
		List<String> embeddedCollections = new ArrayList<String>();

		for ( String property : getPropertyNames() ) {
			Type propertyType = getPropertyType( property );

			if ( propertyType.isAssociationType() ) {
				Joinable associatedJoinable = ( (AssociationType) propertyType ).getAssociatedJoinable( getFactory() );

				// *-to-many
				if ( associatedJoinable.isCollection() ) {
					OgmCollectionPersister inversePersister = (OgmCollectionPersister) associatedJoinable;

					if ( gridDialect.isStoredInEntityStructure( inversePersister.getAssociationKeyMetadata(), inversePersister.getAssociationTypeContext( property ) ) ) {
						embeddedCollections.add( property );
					}
				}
				// *-to-one
				else {
					// TODO: For now I'm adding all *-to-one columns to the projection list; Actually we need to ask the
					// dialect whether it's an embedded association, which we can't find out atm. though as we need all
					// entity persisters to be set up for this
					embeddedCollections.add( property );
				}
			}
			// for embeddables check whether they contain element collections
			else if ( propertyType.isComponentType() ) {
				collectEmbeddedCollectionColumns( (ComponentType) propertyType, property, embeddedCollections );
			}
		}

		return embeddedCollections;
	}

	private void collectEmbeddedCollectionColumns(ComponentType componentType, String dotName, List<String> embeddedCollections) {
		for ( String propertyName : componentType.getPropertyNames() ) {
			Type type = componentType.getSubtypes()[componentType.getPropertyIndex( propertyName )];

			if ( type.isCollectionType() ) {
				embeddedCollections.add( dotName + "." + propertyName );
			}
			else if ( type.isComponentType() ) {
				collectEmbeddedCollectionColumns( (ComponentType) type, dotName + "." + propertyName, embeddedCollections );
			}
		}
	}

	boolean[] getPropertyMightBeMainSideOfBidirectionalAssociation();
	
	default boolean[] initPropertyMightBeMainSideOfBidirectionalAssociation(PersistentClass persistentClass) {
		boolean[] propertyMightBeMainSideOfBidirectionalAssociation = new boolean[getEntityMetamodel().getPropertySpan()];
		final List<Property> propertyClosure = persistentClass.getPropertyClosure();
		
		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			Property prop = propertyClosure.get( propertyIndex );
			Type propertyType = getPropertyTypes()[propertyIndex];
			boolean isStarToOne = propertyType.isAssociationType() && ! propertyType.isCollectionType();

			propertyMightBeMainSideOfBidirectionalAssociation[propertyIndex] = isStarToOne || prop.getValue().isAlternateUniqueKey();
		}

		return propertyMightBeMainSideOfBidirectionalAssociation;
	}

	boolean mightManageInverseAssociations();
	
	default boolean initMightManageInverseAssociations() {
		for ( boolean mightManageInverseAssociation : getPropertyMightBeMainSideOfBidirectionalAssociation() ) {
			if ( mightManageInverseAssociation ) {
				return true;
			}
		}

		return false;
	}

	boolean[] getPropertyMightHaveNavigationalInformation();
	
	default boolean[] initPropertyMightHaveNavigationalInformation() {
		boolean[] propertyMightHaveNavigationalInformation = new boolean[getEntityMetamodel().getPropertySpan()];

		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			Type propertyType = getPropertyTypes()[propertyIndex];
			if ( propertyType.isCollectionType() ) {
				propertyMightHaveNavigationalInformation[propertyIndex] = true;
			}
			else {
				propertyMightHaveNavigationalInformation[propertyIndex] = false;
			}
		}

		return propertyMightHaveNavigationalInformation;
	}

	boolean mightHaveNavigationalInformation();
	
	default boolean initMightHaveNavigationalInformation() {
		for ( boolean hasNavigationalInformation : getPropertyMightHaveNavigationalInformation() ) {
			if ( hasNavigationalInformation ) {
				return true;
			}
		}

		return false;
	}

	default void removeNavigationInformation(Object id, Object entity, SharedSessionContractImplementor session) {
		GridDialect gridDialect = session.getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		boolean[] propertyMightHaveNavigationalInformation = getPropertyMightHaveNavigationalInformation();
		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			if ( propertyMightHaveNavigationalInformation[propertyIndex] ) {
				CollectionType collectionType = (CollectionType) getPropertyTypes()[propertyIndex];
				OgmCollectionPersister collectionPersister = (OgmCollectionPersister) getFactory()
						.getMetamodel().collectionPersister( collectionType.getRole() );

				AssociationPersister associationPersister = new AssociationPersister.Builder( collectionPersister.getOwnerEntityPersister().getMappedClass() )
						.hostingEntity( entity )
						.gridDialect( gridDialect )
						.key( id, collectionPersister.getAttributeMapping().getKeyDescriptor() )
						.associationKeyMetadata( collectionPersister.getAssociationKeyMetadata() )
						.associationTypeContext( collectionPersister.getAssociationTypeContext() )
						.session( session )
						.build();

				Association association = associationPersister.getAssociationOrNull();
				if ( association != null && !association.isEmpty() ) {
					association.clear();
					associationPersister.flushToDatastore();
				}
			}
		}
	}
	


	/**
	 * Removes the given entity from the inverse associations it manages.
	 */
	default void removeFromInverseAssociations(
			Tuple resultset,
			int tableIndex,
			Object id,
			SharedSessionContractImplementor session) {
		new EntityAssociationUpdater( this )
				.id( id )
				.resultset( resultset )
				.session( session )
				.tableIndex( tableIndex )
				.propertyMightRequireInverseAssociationManagement( getPropertyMightBeMainSideOfBidirectionalAssociation() )
				.removeNavigationalInformationFromInverseSide();
	}

	/**
	 * Adds the given entity to the inverse associations it manages.
	 */
	default void addToInverseAssociations(
			Tuple resultset,
			int tableIndex,
			Object id,
			Object entity,
			SharedSessionContractImplementor session) {
		new EntityAssociationUpdater( this )
				.id( id )
				.entity( entity )
				.resultset( resultset )
				.session( session )
				.tableIndex( tableIndex )
				.propertyMightRequireInverseAssociationManagement( getPropertyMightBeMainSideOfBidirectionalAssociation() )
				.addNavigationalInformationForInverseSide();
	}

	/**
	 * Carried over from OGM 5
	 * @return secondary tables are not supported to returns <code>true</code>
	 */
	default boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	/**
	 * Returns the TupleTypeContext associated with this entity type.
	 *
	 * @return the tupleTypeContext
	 */
	TupleTypeContext getTupleTypeContext();
	
	AssociationKeyMetadata getInverseOneToOneAssociationKeyMetadata(String propertyName);

	/**
	 * Returns the {@link TupleContext}.
	 *
	 * @param session the current session, cannot be null. If you don't have a session, you probably want to use {@code getTupleTypeContext()}.
	 * @return the tupleContext for the session
	 */
	default TupleContext getTupleContext(SharedSessionContractImplementor session) {
		return new TupleContextImpl( getTupleTypeContext(), TransactionContextHelper.transactionContext( session ) );
	}

	// Here the RowKey is made of the foreign key columns pointing to the associated entity
	// and the identifier columns of the owner's entity
	// We use the same order as the collection: id column names, foreign key column names
	private String[] buildRowKeyColumnNamesForStarToOne(OgmEntityPersister persister, String[] keyColumnNames) {
		String[] identifierColumnNames = persister.getIdentifierColumnNames();
		int length = identifierColumnNames.length + keyColumnNames.length;
		String[] rowKeyColumnNames = new String[length];
		System.arraycopy( identifierColumnNames, 0, rowKeyColumnNames, 0, identifierColumnNames.length );
		System.arraycopy( keyColumnNames, 0, rowKeyColumnNames, identifierColumnNames.length, keyColumnNames.length );
		return rowKeyColumnNames;
	}
	
	default int getPropertySpan() {
		return getEntityMetamodel().getPropertySpan();
	}
	
	default Type[] getPropertyTypes() {
		return getEntityMetamodel().getPropertyTypes();
	}
}
