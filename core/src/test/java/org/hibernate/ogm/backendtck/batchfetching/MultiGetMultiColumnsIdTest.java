/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.batchfetching;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Tests for dialects implementing the {@link MultigetGridDialect} interface when the id of the entity is mapped on
 * multiple columns.
 *
 * @author Davide D'Alto
 */
@SkipByGridDialect(value = { GridDialectType.INFINISPAN })
public class MultiGetMultiColumnsIdTest extends OgmTestCase {

	private static final EntityKeyMetadata METADATA = new DefaultEntityKeyMetadata( "BoardGame", new String[]{ "name", "publisher" } );

	private static final EntityKey NOT_IN_THE_DB = new EntityKey( METADATA, new Object[]{ "none", "none" } );
	private static final BoardGame DOMINION = new BoardGame( "Rio Grande Games", "Dominion" );
	private static final BoardGame KING_OF_TOKYO = new BoardGame( "Fantasmagoria", "King of Tokyo" );
	private static final BoardGame SPLENDOR = new BoardGame( "Space Cowboys", "Splendor" );

	@Test
	public void testGetTuplesWithoutNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { key( SPLENDOR ), key( DOMINION ), key( KING_OF_TOKYO ) };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				assertThat( tuples.get( 0 ).get( "publisher" ) ).isEqualTo( SPLENDOR.getPublisher() );
				assertThat( tuples.get( 0 ).get( "name" ) ).isEqualTo( SPLENDOR.getName() );

				assertThat( tuples.get( 1 ).get( "publisher" ) ).isEqualTo( DOMINION.getPublisher() );
				assertThat( tuples.get( 1 ).get( "name" ) ).isEqualTo( DOMINION.getName() );

				assertThat( tuples.get( 2 ).get( "publisher" ) ).isEqualTo( KING_OF_TOKYO.getPublisher() );
				assertThat( tuples.get( 2 ).get( "name" ) ).isEqualTo( KING_OF_TOKYO.getName() );

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@Test
	public void testGetTuplesWithNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, key( KING_OF_TOKYO ), NOT_IN_THE_DB, NOT_IN_THE_DB };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				assertThat( tuples.get( 1 ).get( "publisher" ) ).isEqualTo( KING_OF_TOKYO.getPublisher() );
				assertThat( tuples.get( 1 ).get( "name" ) ).isEqualTo( KING_OF_TOKYO.getName() );

				assertThat( tuples.get( 2 ) ).isNull();
				assertThat( tuples.get( 3 ) ).isNull();

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@Test
	public void testGetTuplesWithAllNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	private TupleContext tupleContext(Session session) {
		return new GridDialectOperationContexts.TupleContextBuilder()
				.tupleTypeContext(
						new GridDialectOperationContexts.TupleTypeContextBuilder()
								.selectableColumns( METADATA.getColumnNames() )
								.buildTupleTypeContext() )
				.transactionContext( session )
				.buildTupleContext();
	}

	private EntityKey key(BoardGame boardGame) {
		Object[] values = { boardGame.getName(), boardGame.getPublisher() };
		return new EntityKey( METADATA, values );
	}

	@Before
	public void prepareDataset() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				session.persist( DOMINION );
				session.persist( KING_OF_TOKYO );
				session.persist( SPLENDOR );
				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@After
	public void deleteDataset() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				delete( session, DOMINION );
				delete( session, SPLENDOR );
				delete( session, KING_OF_TOKYO );
				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	private void delete(OgmSession session, BoardGame boardGame) {
		session.delete( session.load( BoardGame.class, new BoardGamePK( boardGame ) ) );
	}

	private MultigetGridDialect multiGetGridDialect() {
		MultigetGridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( MultigetGridDialect.class );
		return gridDialect;
	}

	private void rollback(Transaction tx) {
		if ( tx != null ) {
			tx.rollback();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ BoardGame.class };
	}

	@Entity
	@IdClass(BoardGamePK.class)
	@Table(name = "BoardGame")
	static class BoardGame {

		@Id
		@Column(name = "publisher")
		private String publisher;

		@Id
		@Column(name = "name")
		private String name;

		public BoardGame() {
		}

		public BoardGame(String publisher, String name) {
			this.publisher = publisher;
			this.name = name;
		}

		public String getPublisher() {
			return publisher;
		}

		public void setPublisher(String publisher) {
			this.publisher = publisher;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			result = prime * result + ( ( publisher == null ) ? 0 : publisher.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			BoardGame other = (BoardGame) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			if ( publisher == null ) {
				if ( other.publisher != null ) {
					return false;
				}
			}
			else if ( !publisher.equals( other.publisher ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "BoardGame [publisher=" );
			builder.append( publisher );
			builder.append( ", name=" );
			builder.append( name );
			builder.append( "]" );
			return builder.toString();
		}
	}

	@SuppressWarnings("serial")
	public static class BoardGamePK implements Serializable {

		private String publisher;

		private String name;

		public BoardGamePK() {
		}

		public BoardGamePK(BoardGame boardGame) {
			this.name = boardGame.getName();
			this.publisher = boardGame.getPublisher();
		}

		public String getPublisher() {
			return publisher;
		}

		public void setPublisher(String publisher) {
			this.publisher = publisher;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			result = prime * result + ( ( publisher == null ) ? 0 : publisher.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			BoardGamePK other = (BoardGamePK) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			if ( publisher == null ) {
				if ( other.publisher != null ) {
					return false;
				}
			}
			else if ( !publisher.equals( other.publisher ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "BoardGamePK [publisher=" );
			builder.append( publisher );
			builder.append( ", name=" );
			builder.append( name );
			builder.append( "]" );
			return builder.toString();
		}
	}
}
