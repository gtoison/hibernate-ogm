/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.dialectinvocations;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Toison
 */
public class EmbeddedCollectionTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Test
	public void clearCollection() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		Product product = new Product(
				"product-1",
				"Hybrid 25°",
				new Vendor( "Leaveland", 10 ), new Vendor( "Headaway", 9 )
		);
		session.persist( product );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// load and update
		Product loadedProduct = (Product) session.get( Product.class, "product-1" );
		assertNotNull( "Cannot load persisted object", loadedProduct );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Leaveland", "Headaway" );
		loadedProduct.getVendors().clear();

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();
		
		// load again and check the result
		loadedProduct = (Product) session.get( Product.class, "product-1" );
		assertThat( loadedProduct.getVendors() ).isEmpty();

		transaction.commit();
		session.clear();

		session.close();
		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"getAssociation",
				"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation",
				"executeBatch[group[removeAssociation]]",
				"getTuple",
				"getAssociation"
				);
	}

	@Test
	public void addCollectionElement() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		Product product = new Product(
				"product-2",
				"Hybrid 25°",
				new Vendor( "Leaveland", 10 ), new Vendor( "Headaway", 9 )
		);
		session.persist( product );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// load and update
		Product loadedProduct = (Product) session.get( Product.class, "product-2" );
		assertNotNull( "Cannot load persisted object", loadedProduct );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Leaveland", "Headaway" );
		loadedProduct.getVendors().add( new Vendor( "New vendor", 42 ) );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();
		
		// load again and check the result
		loadedProduct = (Product) session.get( Product.class, "product-2" );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Leaveland", "Headaway", "New vendor" );

		transaction.commit();
		session.clear();
		session.close();

		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"getAssociation",
				"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation",
				"createAssociation",
				"executeBatch[group[removeAssociation,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation"
				);
	}
	
	@Test
	public void removeCollectionElement() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// insert
		Product product = new Product(
				"product-3",
				"Hybrid 25°",
				new Vendor( "Leaveland", 10 ), new Vendor( "Headaway", 9 )
		);
		session.persist( product );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// load and update
		Product loadedProduct = (Product) session.get( Product.class, "product-3" );
		assertNotNull( "Cannot load persisted object", loadedProduct );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Leaveland", "Headaway" );
		loadedProduct.getVendors().removeIf( v -> v.getName().equals( "Leaveland" ) );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();
		
		// load again and check the result
		loadedProduct = (Product) session.get( Product.class, "product-3" );
		assertThat( loadedProduct.getVendors() ).onProperty( "name" ).containsOnly( "Headaway" );

		transaction.commit();
		session.clear();
		session.close();

		assertThat( getOperations() ).containsExactly(
				"createTuple",
				"getAssociation",
				"executeBatch[group[insertOrUpdateTuple,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation",
				"createAssociation",
				"executeBatch[group[removeAssociation,insertOrUpdateAssociation]]",
				"getTuple",
				"getAssociation"
				);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StockItem.class, Product.class };
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.GRID_DIALECT, InvokedOperationsLoggingDialect.class );
	}

	private InvokedOperationsLoggingDialect getOperationsLogger() {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		InvokedOperationsLoggingDialect invocationLogger = GridDialects.getDelegateOrNull(
				gridDialect,
				InvokedOperationsLoggingDialect.class
		);

		return invocationLogger;
	}

	private List<String> getOperations() {
		return getOperationsLogger().getOperations();
	}
}
