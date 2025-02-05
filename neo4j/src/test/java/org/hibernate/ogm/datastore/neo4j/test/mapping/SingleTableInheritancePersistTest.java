/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Davide D'Alto
 */
public class SingleTableInheritancePersistTest extends Neo4jJpaTestCase {

	private Man john;
	private Woman jane;
	private Child susan;
	private Child mark;

	@Before
	public void prepareDB() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		john = new Man();
		jane = new Woman();
		susan = new Child();
		mark = new Child();

		john.name = "John";
		john.wife = jane;
		john.children.add( mark );
		john.children.add( susan );

		jane.name = "Jane";
		jane.husband = john;
		jane.children.add( mark );
		jane.children.add( susan );

		susan.name = "Susan";
		susan.mother = jane;
		susan.father = john;

		mark.name = "Mark";
		mark.mother = jane;
		mark.father = john;

		persist( em, john, jane, mark, susan );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions johnNode = node( "john", Person.TABLE_NAME, ENTITY.name() )
				.property( "TYPE", "MAN" )
				.property( "name", john.name );

		NodeForGraphAssertions janeNode = node( "jane", Person.TABLE_NAME, ENTITY.name() )
				.property( "TYPE", "WOMAN" )
				.property( "name", jane.name );

		NodeForGraphAssertions susanNode = node( "susan", Person.TABLE_NAME, ENTITY.name() )
				.property( "TYPE", "CHILD" )
				.property( "name", susan.name );

		NodeForGraphAssertions markNode = node( "mark", Person.TABLE_NAME, ENTITY.name() )
				.property( "TYPE", "CHILD" )
				.property( "name", mark.name );

		RelationshipsChainForGraphAssertions johnWife = johnNode.relationshipTo( janeNode, "wife" );

		RelationshipsChainForGraphAssertions susanMother = susanNode.relationshipTo( janeNode, "mother" );
		RelationshipsChainForGraphAssertions susanFather = susanNode.relationshipTo( johnNode, "father" );

		RelationshipsChainForGraphAssertions markMother = markNode.relationshipTo( janeNode, "mother" );
		RelationshipsChainForGraphAssertions markFather = markNode.relationshipTo( johnNode, "father" );

		assertThatOnlyTheseNodesExist( johnNode, janeNode, markNode, susanNode );
		assertThatOnlyTheseRelationshipsExist( susanMother, susanFather, markMother, markFather, johnWife );

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class, Man.class, Woman.class, Child.class };
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
	@Table( name = Person.TABLE_NAME )
	class Person {

		public static final String TABLE_NAME = "Person";

		@Id
		String name;
	}

	@Entity
	@DiscriminatorValue("MAN")
	class Man extends Person {

		@OneToOne
		Woman wife;

		@OneToMany(mappedBy = "father")
		List<Child> children = new ArrayList<>();
	}

	@Entity
	@DiscriminatorValue("WOMAN")
	class Woman extends Person {

		@OneToOne(mappedBy = "wife")
		Man husband;

		@OneToMany(mappedBy = "mother")
		List<Child> children = new ArrayList<>();
	}

	@Entity
	@DiscriminatorValue("CHILD")
	class Child extends Person {

		@OneToOne
		Woman mother;

		@OneToOne
		Man father;
	}
}
