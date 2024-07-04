/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.depositor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "ACCOUNT")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "TYPE")
public abstract class Account {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	@Version
	@Column(name = "VERSION")
	private Long version;

	@ManyToOne(optional = false, cascade = {
			CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH
	})
	@JoinColumn(name = "DEPOSITOR_ID", updatable = false)
	private Depositor depositor;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ACCOUNT_ID")
	private List<AccountEntry> entries = new ArrayList<>();

	protected Account() {
		// for JPA
	}

	public Account(final Depositor depositor) {
		this.depositor = depositor;
		depositor.addAccount( this );
	}

	public Depositor getDepositor() {
		return depositor;
	}

	protected void setDepositor(final Depositor depositor) {
		this.depositor = depositor;
	}

	protected List<AccountEntry> getEntries() {
		return Collections.unmodifiableList( entries );
	}

	protected void addEntry(final AccountEntry entry) {
		entries.add( entry );
	}

	public double getBalance() {
		double balance = 0;
		final Iterator<AccountEntry> it = entries.iterator();
		while ( it.hasNext() ) {
			final AccountEntry entry = it.next();
			balance += entry.getAmount();
		}
		return balance;
	}
}
