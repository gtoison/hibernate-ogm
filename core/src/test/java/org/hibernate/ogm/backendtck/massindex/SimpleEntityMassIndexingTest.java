/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.hsearch.Insurance;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.scope.LuceneIndexScope;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class SimpleEntityMassIndexingTest extends OgmTestCase {

	@Test
	public void testSimpleEntityMassIndexing() throws Exception {
		{
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			Insurance insurance = new Insurance();
			insurance.setName( "Insurance Corporation" );
			session.persist( insurance );
			transaction.commit();
			session.clear();
			session.close();
		}
		{
			purgeAll( Insurance.class );
			startAndWaitMassIndexing( Insurance.class );
		}
		{
			Session session = openSession();
			SearchSession searchSession = Search.session( session );
			
			List<Insurance> list = searchSession.search( Insurance.class ).where( f -> f.wildcard().field( "name" ).matching( "ins*" ) ).fetchAllHits();
			Transaction transaction = session.beginTransaction();
			
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getName() ).isEqualTo( "Insurance Corporation" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	@Test
	@SkipByGridDialect(value = { MONGODB }, comment = "Uses embedded key which is currently not supported by the db query parsers")
	public void testEntityWithCompositeIdMassIndexing() throws Exception {
		{
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
			session.persist( news );
			transaction.commit();
			session.clear();
			session.close();
		}
		{
			purgeAll( IndexedNews.class );
			startAndWaitMassIndexing( IndexedNews.class );
		}
		{
			// Assert index creation
			Session session = openSession();
			SearchSession searchSession = Search.session( session );
			List<IndexedNews> list = searchSession.search( IndexedNews.class ).where( f -> f.wildcard().field( "newsId" ).matching( "tit*" ) ).fetchAllHits();
			Transaction transaction = session.beginTransaction();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getContent() ).isEqualTo( "content" );
			assertThat( list.get( 0 ).getNewsId().getTitle() ).isEqualTo( "title" );
			assertThat( list.get( 0 ).getNewsId().getAuthor() ).isEqualTo( "author" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	private void startAndWaitMassIndexing(Class<?> entityType) throws InterruptedException, IOException {
		Session session = openSession();
		SearchSession searchSession = Search.session( session );
		searchSession.massIndexer( entityType ).purgeAllOnStart( true ).startAndWait();
		
		SearchMapping mapping = Search.mapping( session.getEntityManagerFactory() ); 
		LuceneIndexScope indexScope = mapping.scope( entityType ).extension( LuceneExtension.get() ); 
		try ( IndexReader indexReader = indexScope.openIndexReader() ) { 
		    int numDocs = indexReader.numDocs();
		    assertThat( numDocs ).isGreaterThan( 0 );
		} finally {
			session.close();
		}
	}

	private void purgeAll(Class<?> entityType) throws IOException {
		Session session = openSession();
		SearchSession searchSession = Search.session( session );
		searchSession.workspace( entityType ).flush();
		searchSession.indexingPlan().execute();
		
		SearchMapping mapping = Search.mapping( session.getEntityManagerFactory() ); 
		LuceneIndexScope indexScope = mapping.scope( entityType ).extension( LuceneExtension.get() ); 
		try ( IndexReader indexReader = indexScope.openIndexReader() ) { 
		    int numDocs = indexReader.numDocs();
		    assertThat( numDocs ).isEqualTo( 0 );
		} finally {
			session.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class, IndexedNews.class, IndexedLabel.class };
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( "hibernate.search.backend.directory.type", "local-heap" );
		TestHelper.enableCountersForInfinispan( settings );
	}
}
