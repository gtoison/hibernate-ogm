/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import static org.hamcrest.CoreMatchers.isA;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import jakarta.persistence.OptimisticLockException;

/**
 * Test for detecting concurrent updates by dialects which support atomic find/update semantics or have their own
 * optimistic locking scheme.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(value = INFINISPAN_REMOTE, comment = "At the moment Infinispan HotRod transactions are always pessimistic and REPEATABLE_READ")
public class OptimisticLockingTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final Class<? extends Throwable> lockExceptionClass;

	private enum LatchAction { DECREASE_AND_WAIT, IGNORE }

	private ThreadFactory threadFactory;

	private final CountDownLatch deleteLatch = new CountDownLatch( 2 );

	public OptimisticLockingTest() {
		this( OptimisticLockException.class );
	}

	protected OptimisticLockingTest(Class<? extends Throwable> lockExceptionClass) {
		this.lockExceptionClass = lockExceptionClass;
	}

	@Before
	public void setupThreadFactory() {
		threadFactory = new ThreadFactoryBuilder().setNameFormat( "ogm-test-thread-%d" ).build();
	}

	@After
	public void cleanUp() {
		removePlanet();
		removePulsar();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to updating it and comparing
	 * its version.
	 */
	@Test
	public void updatingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( lockExceptionClass );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and update it
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		entity.setName( "Uranus" );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the update
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests "real" optimistic locking by means of atomic find-and-modify semantics if supported by the datastore.
	 */
	@Test
	@SkipByGridDialect(
			value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE }
	)
	public void updatingEntityUsingOldVersionCausesExceptionUsingAtomicFindAndUpdate() throws Throwable {
		thrown.expectCause( isA( lockExceptionClass ) );

		persistPlanet();

		// for the first update, the test dialect waits a bit between read and write, so the second update will take
		// place in between, causing the exception
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.DECREASE_AND_WAIT );
		Future<?> future2 = updateInSeparateThread( Planet.class, "planet-1", "Uranus", LatchAction.DECREASE_AND_WAIT );

		future2.get();
		future1.get();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to removing it and comparing
	 * its version.
	 */
	@Test
	public void deletingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( lockExceptionClass );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and delete it
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		session.delete( entity );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the removal
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests "real" optimistic locking by means of atomic find-and-delete semantics if supported by the datastore.
	 */
	@Test
	@SkipByGridDialect(
			value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE }
	)
	public void deletingEntityUsingOldVersionCausesExceptionUsingAtomicFindAndDelete() throws Throwable {
		thrown.expectCause( isA( lockExceptionClass ) );

		persistPlanet();

		// for the delete, the test dialect waits a bit between read and delete, so the update will take place in
		// between, causing the exception
		Future<?> future1 = removePlanetInSeparateThread();
		Future<?> future2 = updateInSeparateThread( Planet.class, "planet-1", "Uranus", LatchAction.DECREASE_AND_WAIT );

		future2.get();
		future1.get();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to updating it and comparing
	 * its version.
	 */
	@Test
	public void updatingEntityUsingOldEntityStateCausesException() throws Throwable {
		thrown.expect( lockExceptionClass );

		persistPulsar();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and update it
		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		entity.setName( "PSR J0537-6910" );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Pulsar.class, "pulsar-1", "PSR B1257+12", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the update
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to deleting it and comparing
	 * its version.
	 */
	@Test
	public void deletingEntityUsingOldEntityStateCausesException() throws Throwable {
		thrown.expect( lockExceptionClass );

		persistPulsar();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and delete it
		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		session.delete( entity );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Pulsar.class, "pulsar-1", "PSR B1257+12", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the removal
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	@Test
	public void mergingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( lockExceptionClass );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );

		commitTransactionAndPropagateExceptions( session, transaction );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		session = openSession();
		transaction = session.beginTransaction();

		// merging back the previously loaded version will cause an exception
		try {
			entity = (Planet) session.merge( entity );
		}
		catch ( Exception e ) {
			throw e;
		}
		finally {
			commitTransactionAndPropagateExceptions( session, transaction );
		}
	}

	private Future<?> updateInSeparateThread(final Class<? extends Nameable> type, final String id, final String newName, final LatchAction latchAction) throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Runnable() {

			@Override
			public void run() {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				// load the entity and update it
				Nameable entity = (Nameable) session.get( type, id );
				entity.setName( newName );

				if ( latchAction == LatchAction.DECREASE_AND_WAIT ) {
					countDownAndAwaitLatch();
				}

				transaction.commit();
				session.close();
			}
		} );
	}

	private Future<?> removePlanetInSeparateThread() throws Exception {
		return Executors.newSingleThreadExecutor( threadFactory ).submit( new Runnable() {

			@Override
			public void run() {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				Planet entity;

				// load the entity and remove it
				entity = (Planet) session.get( Planet.class, "planet-1" );

				// the latch makes sure we have loaded the entity in both of the sessions before we're going to delete it
				countDownAndAwaitLatch();

				session.delete( entity );

				transaction.commit();
				session.close();
			}
		} );
	}

	private Planet persistPlanet() {
		Session session = openSession();

		session.beginTransaction();

		Planet planet = new Planet( "planet-1", "Pluto" );
		session.persist( planet );

		session.getTransaction().commit();
		session.close();

		return planet;
	}

	public void removePlanet() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		if ( entity != null ) {
			session.delete( entity );
		}

		transaction.commit();
	}

	private Pulsar persistPulsar() {
		Session session = openSession();

		session.beginTransaction();

		Pulsar pulsar = new Pulsar( "pulsar-1", "PSR 1919+21", 1.33 );
		session.persist( pulsar );

		session.getTransaction().commit();
		session.close();

		return pulsar;
	}

	public void removePulsar() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		if ( entity != null ) {
			session.delete( entity );
		}

		transaction.commit();
	}

	private void commitTransactionAndPropagateExceptions(Session session, Transaction transaction) throws Exception {
		try {
			if ( !transaction.getRollbackOnly() ) {
				transaction.commit();
			}
			else {
				transaction.rollback();
			}
		}
		catch (Exception e) {
			if ( transaction.getStatus() != TransactionStatus.NOT_ACTIVE ) {
				transaction.rollback();
			}
			throw e;
		}
		finally {
			session.close();
		}
	}

	private void countDownAndAwaitLatch() {
		deleteLatch.countDown();
		try {
			deleteLatch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.GRID_DIALECT, TestDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Planet.class, Pulsar.class };
	}

	@SuppressWarnings("serial")
	public static class TestDialect extends ForwardingGridDialect<Serializable> {

		public TestDialect(DatastoreProvider provider) {
			super( TestHelper.getCurrentGridDialect( provider ) );
		}

		@Override
		public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, Tuple tuple, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.updateTupleWithOptimisticLock( entityKey, oldVersion, tuple, tupleContext );
		}

		@Override
		public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.removeTupleWithOptimisticLock( entityKey, oldVersion, tupleContext );
		}

		private void waitALittleBit() {
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}
	}
}
