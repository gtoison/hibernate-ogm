/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.id.impl.OgmIdentityColumnSupport;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;

/**
 * A pseudo {@link Dialect} implementation which exposes the current {@link GridDialect}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OgmDialect extends Dialect {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final GridDialect gridDialect;

	public OgmDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
	}

	/**
	 * Returns the current {@link GridDialect}.
	 * <p>
	 * Intended for usage in code interacting with ORM SPIs which only provide access to the {@link Dialect} but not the
	 * service registry. Other code should obtain the grid dialect from the service registry.
	 *
	 * @return the current grid dialect.
	 */
	public GridDialect getGridDialect() {
		return gridDialect;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new OgmIdentityColumnSupport( gridDialect );
	}
	
	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory();
	}
	
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contribute( typeContributions, serviceRegistry );
		
		gridDialect.contributeTypes( typeContributions, serviceRegistry );
	}
}
