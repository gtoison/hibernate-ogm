/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Account {
	private String login;
	private String password;
	private Address homeAddress;
	private Integer version;

	@Id
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Address getHomeAddress() {
		return homeAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	@Version
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
