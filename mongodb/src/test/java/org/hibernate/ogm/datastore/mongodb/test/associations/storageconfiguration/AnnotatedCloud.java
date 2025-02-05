/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.storageconfiguration;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorage;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

/**
 * @author Gunnar Morling
 */
@Entity
@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
@AssociationDocumentStorage(AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION)
public class AnnotatedCloud {
	private String id;
	private String type;
	private double length;
	private Set<SnowFlake> producedSnowFlakes = new HashSet<SnowFlake>();
	private Set<SnowFlake> backupSnowFlakes = new HashSet<SnowFlake>();

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	@OneToMany
	@JoinTable
	public Set<SnowFlake> getProducedSnowFlakes() {
		return producedSnowFlakes;
	}

	public void setProducedSnowFlakes(Set<SnowFlake> producedSnowFlakes) {
		this.producedSnowFlakes = producedSnowFlakes;
	}

	@OneToMany
	@JoinTable
	@AssociationDocumentStorage(AssociationDocumentStorageType.GLOBAL_COLLECTION)
	public Set<SnowFlake> getBackupSnowFlakes() {
		return backupSnowFlakes;
	}

	public void setBackupSnowFlakes(Set<SnowFlake> backupSnowFlakes) {
		this.backupSnowFlakes = backupSnowFlakes;
	}
}
