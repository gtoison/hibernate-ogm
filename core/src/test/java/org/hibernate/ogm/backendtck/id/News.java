/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@Entity
public class News {

	@EmbeddedId
	private NewsID newsId;
	private String content;

	@OneToMany(cascade =  CascadeType.ALL )
	@JoinColumns({ @JoinColumn(name = "news_topic_fk", referencedColumnName = "newsid.title", nullable = false),
			@JoinColumn(name = "news_author_fk", referencedColumnName = "newsid.author", nullable = false) })
	private List<Label> labels;

	public News() {
		super();
	}

	public News(NewsID newsId, String content, List<Label> labels) {
		this.newsId = newsId;
		this.content = content;
		this.labels = labels;
	}


	public NewsID getNewsId() { return newsId; }
	public void setNewsId(NewsID newsId) { this.newsId = newsId; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public List<Label> getLabels() { return labels;	}
	public void setLabels(List<Label> labels) {	this.labels = labels; }

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		News news = (News) o;

		if ( content != null ? !content.equals( news.content ) : news.content != null ) {
			return false;
		}
		if ( newsId != null ? !newsId.equals( news.newsId ) : news.newsId != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = newsId != null ? newsId.hashCode() : 0;
		result = 31 * result + ( content != null ? content.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "News [newsId=" );
		builder.append( newsId );
		builder.append( ", content=" );
		builder.append( content );
		builder.append( "]" );
		return builder.toString();
	}
}
