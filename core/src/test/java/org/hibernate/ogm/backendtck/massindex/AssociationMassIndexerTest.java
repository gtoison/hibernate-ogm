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
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.scope.LuceneIndexScope;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.Test;

import jakarta.persistence.EntityManager;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class AssociationMassIndexerTest extends OgmJpaTestCase {

	@Test
	@SkipByGridDialect(value = { MONGODB }, comment = "Uses embedded key which is currently not supported by the db query parsers")
	public void testEntityWithAssociationMassIndexing() throws Exception {
		populateDatastore();
		purgeAll( IndexedNews.class, IndexedLabel.class );
		startAndWaitMassIndexing( IndexedNews.class, IndexedLabel.class );

		assertEntityHasBeenIndexed();
		assertAssociatedElementsHaveBeenIndexed();
	}

	private void populateDatastore() throws Exception {
		List<IndexedLabel> labes = Arrays.asList( new IndexedLabel( "massindex" ), new IndexedLabel( "test" ) );
		IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
		news.setLabels( labes );
		EntityManager em = createEntityManager();
		em.getTransaction().begin();
		em.persist( news );
		em.getTransaction().commit();
		em.close();
	}

	private void assertEntityHasBeenIndexed() throws Exception {
		EntityManager em = createEntityManager();
		SearchSession fullTextEm = Search.session( em );
		em.getTransaction().begin();
		List<IndexedNews> list = fullTextEm.search( IndexedNews.class )
				.where( f -> f.wildcard().field( "newsId" ).matching( "tit*"))
				.fetchAllHits();
						
		assertThat( list ).hasSize( 1 );

		List<IndexedLabel> labels = list.get( 0 ).getLabels();
		assertThat( labels ).hasSize( 2 );
		assertThat( contains( labels, "massindex" ) ).isTrue();
		assertThat( contains( labels, "test" ) ).isTrue();
		em.getTransaction().commit();
		em.close();
	}

	@SuppressWarnings("unchecked")
	private void assertAssociatedElementsHaveBeenIndexed() throws Exception {
		EntityManager em = createEntityManager();
		SearchSession fullTextEm = Search.session( em );
		em.getTransaction().begin();
		
		{
			List<IndexedLabel> labels = fullTextEm.search( IndexedLabel.class )
					.where( f -> f.wildcard().field( "name" ).matching( "tes*"))
					.fetchAllHits();
			
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "test" ) ).isTrue();
		}
		{
			List<IndexedLabel> labels = fullTextEm.search( IndexedLabel.class )
					.where( f -> f.wildcard().field( "name" ).matching( "mas*"))
					.fetchAllHits();
			
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "massindex" ) ).isTrue();
		}
		em.getTransaction().commit();
		em.close();
	}

	private boolean contains(List<IndexedLabel> list, String label) {
		for ( IndexedLabel indexedLabel : list ) {
			if ( indexedLabel.getName().equals( label ) ) {
				return true;
			}
		}
		return false;
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

	private void startAndWaitMassIndexing(Class<?>... entityTypes) throws InterruptedException, IOException {
		EntityManager em = createEntityManager();
		SearchSession fullTextEm = Search.session( em );
		fullTextEm.massIndexer( entityTypes ).purgeAllOnStart( true ).startAndWait();
		
		SearchMapping mapping = Search.mapping( em.getEntityManagerFactory() ); 
		LuceneIndexScope indexScope = mapping.scope( Arrays.asList( entityTypes ) ).extension( LuceneExtension.get() ); 
		try ( IndexReader indexReader = indexScope.openIndexReader() ) { 
		    int numDocs = indexReader.numDocs();
		    assertThat( numDocs ).isGreaterThan( 0 );
		} finally {
			em.close();
		}
		
	}

	private void purgeAll(Class<?>... entityTypes) throws Exception {
		EntityManager em = createEntityManager();
		SearchSession fullTextEm = Search.session( em );
		for ( Class<?> entityType : entityTypes ) {
			fullTextEm.workspace( entityType ).purge();
			fullTextEm.indexingPlan().execute();
		}
		
		SearchMapping mapping = Search.mapping( em.getEntityManagerFactory() ); 
		LuceneIndexScope indexScope = mapping.scope( Arrays.asList( entityTypes ) ).extension( LuceneExtension.get() ); 
		try ( IndexReader indexReader = indexScope.openIndexReader() ) { 
		    int numDocs = indexReader.numDocs();
		    assertThat( numDocs ).isEqualTo( 0 );
		} finally {
			em.close();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IndexedNews.class, IndexedLabel.class };
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		super.configure( info );
		info.getProperties().setProperty( "hibernate.search.backend.directory.type", "local-heap" );
		TestHelper.enableCountersForInfinispan( info.getProperties() );
	}
}
