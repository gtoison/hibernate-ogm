/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import jakarta.persistence.criteria.Predicate.BooleanOperator;

/**
 * 
 * @author Guillaume Toison
 */
public class MongoDBSemanticQueryParser extends BaseSemanticQueryWalker {

	private final MappingMetamodel domainModel;
	private final EntityPersister entityDescriptor;
	
	public MongoDBSemanticQueryParser(SqmSelectStatement<?> sqm, DomainQueryExecutionContext executionContext) {
		domainModel = executionContext.getSession().getSessionFactory().getMappingMetamodel();
		
		SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		SqmRoot<?> sqmRoot = (SqmRoot<?>) sqm.getSelection();
		entityDescriptor = factory
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( sqmRoot.getModel().getHibernateEntityName() );
	}

	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, SqmSelectStatement<?> sqm,
			DomainQueryExecutionContext executionContext) {

		SqmQuerySpec<?> queryPart = (SqmQuerySpec<?>) sqm.getQueryPart();
		
		String collection = entityDescriptor.getIdentifierTableName();
		Document query = parse( queryPart.getWhereClause() );
		Document projection = null;
		Document orderBy = null;
		List<String> unwinds = null;
		Operation operation = Operation.FIND;
		AggregationRenderer aggregation = null;
		
		return new MongoDBQueryParsingResult(sqm.getSelection().getJavaType(), collection, query, projection, orderBy, unwinds, operation, aggregation );
	
	}
	
	private Document parse(SqmWhereClause whereClause) {
		if (whereClause == null) {
			return new Document();
		}
		
		return parse( whereClause.getPredicate(), false );
	}

	private Document parse(SqmPredicate predicate, boolean negated) {
		if (predicate == null) {
			return new Document();
		}
		else if ( predicate instanceof SqmJunctionPredicate ) {
			return parseJunctionPredicate( (SqmJunctionPredicate) predicate, negated );
		}
		else if ( predicate instanceof SqmComparisonPredicate ) {
			return parseComparisonPredicate( (SqmComparisonPredicate) predicate, negated );
		}
		else if ( predicate instanceof SqmLikePredicate ) {
			return parseLikePredicate( (SqmLikePredicate) predicate, negated );
		}
		else if ( predicate instanceof SqmNullnessPredicate ) {
			return parseSqmNullnessPredicate( (SqmNullnessPredicate) predicate, negated );
		}
		
		throw new UnsupportedOperationException( "Unsupported predicate: " + predicate.asLoggableText() );
	}

	private Document parseJunctionPredicate(SqmJunctionPredicate junctionPredicate, boolean negated) {
		BooleanOperator operator = junctionPredicate.getOperator();
		
		List<Document> elements = new ArrayList<Document>();
		for (SqmPredicate elementPredicate : junctionPredicate.getPredicates()) {
			elements.add( parse( elementPredicate, junctionPredicate.isNegated() ^ negated ) );
		}
		
		if (operator == BooleanOperator.AND ^ junctionPredicate.isNegated() ^ negated) {
			return new Document( "$and", elements );
		}
		else {
			return new Document( "$or", elements );
		}
	}

	private Document parseComparisonPredicate(SqmComparisonPredicate comparisonPredicate, boolean negated) {
		SqmExpression<?> lh = comparisonPredicate.getLeftHandExpression();
		SqmExpression<?> rh = comparisonPredicate.getRightHandExpression();
		
		boolean lhLiteral = lh instanceof SqmLiteral<?>;
		boolean rhLiteral = rh instanceof SqmLiteral<?>;

		ComparisonOperator operator = comparisonPredicate.getSqmOperator();
		
		if (rhLiteral && lhLiteral) {
			// We could probably make this work by comparing the values without even hitting the database
			throw new UnsupportedOperationException( "Literal comparison not supported" );
		}
		if (rhLiteral) {
			return parseComparisonToLiteral( lh, (SqmLiteral<?>) rh, operator, negated, false);
		}
		else if (lhLiteral) {
			return parseComparisonToLiteral( rh, (SqmLiteral<?>) lh, operator, negated, true);
		}
		else {
			// TODO implement this
			throw new UnsupportedOperationException( "Expressions comparison not supported" );
		}
	}

	/**
	 * @param rh The right hand expression
	 * @param lh The left hand literal
	 * @param operator The {@link ComparisonOperator}
	 * @param negated <code>true</code> if the filter must be negated
	 * @param inverted <code>true</code> if the left and right hands were inverted
	 * @return The filter document
	 */
	private Document parseComparisonToLiteral(SqmExpression<?> rh, SqmLiteral<?> lh, ComparisonOperator operator, boolean negated, boolean inverted) {
		String propertyName = determineValueMapping( rh );
		Object value = lh.getLiteralValue();
		
		String mdbOperator;
		
		switch (operator) {
		case EQUAL : {
			if (!negated) {
				return new Document( propertyName, value );
			}
			else {
				mdbOperator = "$ne";
			}
		}
		
		case NOT_EQUAL : {
			if (!negated) {
				mdbOperator = "$ne";
			}
			else {
				return new Document( propertyName, value );
			}
		}
		
		case LESS_THAN : {
			if (!negated && !inverted) {
				mdbOperator = "$lt";
			}
			else if (negated && inverted) {
				mdbOperator = "$lte";
			}
			else {
				mdbOperator = "$gte";
			}
			break;
		}
		
		case LESS_THAN_OR_EQUAL : {
			if (!negated && !inverted) {
				mdbOperator = "$lte";
			}
			else if (negated && inverted) {
				mdbOperator = "$lt";
			}
			else {
				mdbOperator = "$gt";
			}
			break;
		}

		case GREATER_THAN : {
			if (!negated && !inverted) {
				mdbOperator = "$gt";
			}
			else if (negated && inverted) {
				mdbOperator = "$gte";
			}
			else {
				mdbOperator = "$lte";
			}
			break;
		}
		
		case GREATER_THAN_OR_EQUAL : {
			if (!negated && !inverted) {
				mdbOperator = "$gte";
			}
			else if (negated && inverted) {
				mdbOperator = "$gt";
			}
			else {
				mdbOperator = "$lt";
			}
			break;
		}
		default:
			throw new IllegalArgumentException(	"Unexpected operator: " + operator );
		}

		return new Document( propertyName, new Document( mdbOperator, value ) );
	}

	private Document parseLikePredicate(SqmLikePredicate likePredicate, boolean negated) {
		Character escapeCharacter = null;
		if (likePredicate.getEscapeCharacter() instanceof SqmLiteral<?>) {
			escapeCharacter = ((SqmLiteral<Character>) likePredicate.getEscapeCharacter()).getLiteralValue();
		}
		
		String patternValue = ((SqmLiteral<String>) likePredicate.getPattern()).getLiteralValue();
		String propertyName = determineValueMapping( likePredicate.getMatchExpression() );
				
		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( escapeCharacter );
		Pattern pattern = converter.getRegExpFromLikeExpression( patternValue );
		
		if (likePredicate.isNegated() ^ negated) {
			return new Document( propertyName, new Document( "$not", pattern ) );
		}
		else {
			return new Document( propertyName, pattern );
		}
	}

	private Document parseSqmNullnessPredicate(SqmNullnessPredicate predicate, boolean negated) {
		String propertyName = determineValueMapping( predicate.getExpression() );
		
		return new Document( propertyName, new Document( "$exists", !negated ) );
	}

	private String determineValueMapping(SqmExpression<?> sqmExpression) {
		if ( sqmExpression instanceof SqmPath ) {
			SqmPath<?> path = (SqmPath<?>) sqmExpression;
			BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findSubPart( path.getReferencedPathSource().getPathName(), null );
			
			return attribute.getSelectableName();
		}
		
		throw new UnsupportedOperationException( "Unsupported expression: " + sqmExpression );
	}
}
