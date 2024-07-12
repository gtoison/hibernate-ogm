/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.gen;

import java.util.Objects;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Indexed
@GenericGenerator(name = "customGen", strategy = "org.hibernate.ogm.backendtck.id.gen.CustomIdGenerator")
public class CustomIdGeneratorEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "customGen")
	String id;

	CustomIdGeneratorEntity(String id) {
		this.id = id;
	}

	public CustomIdGeneratorEntity() {
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		CustomIdGeneratorEntity that = (CustomIdGeneratorEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
