/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jdbc.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Utility methods to map from/to jdbc/ogm using the {@link JdbcMapping}
 * 
 * @author Guillaume Toison
 */
public class JdbcOgmMapper {

	public static EntityKey extractKey(Object restrictedValue, SharedSessionContractImplementor session,
			final EntityIdentifierMapping restrictivePart) {
		int columnsCount = restrictivePart.getJdbcTypeCount();
		BinderCapturingPreparedStatement st = new BinderCapturingPreparedStatement(
				columnsCount );
		String[] keyColumnName = new String[columnsCount];

		JdbcValueConsumer keyValueConsumer = new JdbcValueConsumer() {

			@Override
			public void consume(int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
				bindValue( st, session, value, valueIndex, jdbcValueMapping.getJdbcMapping() );
				keyColumnName[valueIndex] = jdbcValueMapping.getSelectionExpression();
			}
		};

		restrictivePart.breakDownJdbcValues( restrictedValue, keyValueConsumer, session );

		String tableName = restrictivePart.getContainingTableExpression();
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata(
				tableName,
				keyColumnName );
		EntityKey entityKey = new EntityKey(
				keyMetadata,
				st.getValues() );

		return entityKey;
	}

	public static void updateTuple(JdbcValueBindings valueBindings, String tableName, Tuple tuple,
			WrapperOptions options) {
		for (Binding binding : valueBindings.getBindingGroup( tableName ).getBindings()) {
			BinderCapturingPreparedStatement st = new BinderCapturingPreparedStatement(	1 );
			bindValue( st, options, binding.getValue(), 0, binding.getValueDescriptor().getJdbcMapping() );
			tuple.put( binding.getColumnName(), st.getValues()[0] );
		}
	}

	public static void updateTuple(ModelPart mapping, Tuple tuple, Object domainValue,
			SharedSessionContractImplementor session) {
		JdbcValueConsumer consumer = new JdbcValueConsumer() {
			
			@Override
			public void consume(int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
				tuple.put( jdbcValueMapping.getSelectionExpression(), value );
			}
		};
		
		mapping.breakDownJdbcValues( domainValue, consumer, session );
	}

	/**
	 * @param options Typically the {@link Session}
	 */
	public static EntityKey buildEntityKey(JdbcValueBindings valueBindings, String tableName, String[] keyColumnNames,
			EntityKeyMetadata keyMetadata, WrapperOptions options) {
		Object[] keyValues = new Object[keyColumnNames.length];
		BinderCapturingPreparedStatement st = new BinderCapturingPreparedStatement(
				keyColumnNames.length );
		for (Binding binding : valueBindings.getBindingGroup( tableName ).getBindings()) {
			for (int i = 0; i < keyColumnNames.length; i++) {
				if ( keyColumnNames[i].equals( binding.getColumnName() ) ) {
					Object value = binding.getValue();
					
					bindValue( st, options, value, i, binding.getValueDescriptor().getJdbcMapping() );
					keyValues[i] = binding.getValueDescriptor().getJdbcMapping().convertToRelationalValue( value );
				}
			}
		}

		return new EntityKey(
				keyMetadata,
				st.getValues() );
	}

	/**
	 * @param options    Typically the {@link Session}
	 * @param valueIndex the zero-based index
	 */
	private static void bindValue(PreparedStatement st, WrapperOptions options, Object value, int valueIndex,
			JdbcMapping mapping) {
		try {
			mapping.getJdbcValueBinder().bind( st, value, valueIndex + 1, options );
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
