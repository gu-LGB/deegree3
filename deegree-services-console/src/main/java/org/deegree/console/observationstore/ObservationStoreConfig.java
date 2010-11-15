/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.console.observationstore;

import org.deegree.console.ManagedXMLConfig;
import org.deegree.observation.persistence.ObservationStoreProvider;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:ionita@lat-lon.de">Andrei Ionita</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class ObservationStoreConfig extends ManagedXMLConfig {

    /**
     * 
     */
    private static final long serialVersionUID = 5042571409250389913L;

    public ObservationStoreConfig( String id, boolean active, boolean ignore, ObservationStoreConfigManager manager,
                                   ObservationStoreProvider provider ) {
        super( id, active, ignore, manager, provider.getConfigSchema(), provider.getConfigTemplate() );
    }

    public String createTables() {
        throw new RuntimeException(
                                    "Currently it is not possible to create tables/ insert data into the observation databases. Please check back soon!" );
    }

    public String showInfo() {
        throw new RuntimeException(
                                    "Currently this feature has no definition for an observation store. Please check back soon!" );
    }
    
    @Override
    public String getOutcome() {
        return "observationStore";
    }


}
