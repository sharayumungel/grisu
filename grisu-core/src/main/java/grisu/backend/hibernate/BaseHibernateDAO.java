/* Copyright 2007, 2008 ARCS
 *
 * This file is part of Grisu.
 * Grisu is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * any later version.

 * Grisu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Grisu; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package grisu.backend.hibernate;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access object (DAO) for domain model.
 * 
 * @author MyEclipse - Hibernate Tools
 */
public class BaseHibernateDAO implements IBaseHibernateDAO {

	static Logger myLogger = LoggerFactory.getLogger(BaseHibernateDAO.class
			.getName());

	public final Session getCurrentSession() {
		try {

			// TODO disable that check for mysql
			HibernateSessionFactory.ensureDerbyServerIsUp();

			return HibernateSessionFactory.getSessionFactory()
					.getCurrentSession();
		} catch (final Exception e) {
			myLogger.error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	public final Session getSession() {
		return HibernateSessionFactory.getSessionFactory().openSession();
	}

}
