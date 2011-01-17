//$HeadURL$
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
package org.deegree.services.wfs;

import static org.deegree.protocol.wfs.WFSConstants.VERSION_100;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_110;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_200;
import static org.deegree.protocol.wfs.WFSConstants.WFS_200_NS;
import static org.deegree.protocol.wfs.WFSConstants.WFS_NS;

import java.net.URL;

import org.deegree.commons.config.ResourceManager;
import org.deegree.commons.tom.ows.Version;
import org.deegree.feature.persistence.FeatureStoreManager;
import org.deegree.protocol.wfs.WFSConstants.WFSRequestType;
import org.deegree.services.OWS;
import org.deegree.services.OWSProvider;
import org.deegree.services.controller.ImplementationMetadata;

/**
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class WFSProvider implements OWSProvider<WFSRequestType> {

    protected static final ImplementationMetadata<WFSRequestType> IMPLEMENTATION_METADATA = new ImplementationMetadata<WFSRequestType>() {
        {
            supportedVersions = new Version[] { VERSION_100, VERSION_110, VERSION_200 };
            handledNamespaces = new String[] { WFS_NS, WFS_200_NS };
            handledRequests = WFSRequestType.class;
            supportedConfigVersions = new Version[] { Version.parseVersion( "3.0.0" ), Version.parseVersion( "3.1.0" ) };
            serviceName = "WFS";
        }
    };

    public String getConfigNamespace() {
        return "http://www.deegree.org/services/wfs";
    }

    public URL getConfigSchema() {
        return WFSProvider.class.getResource( "/META-INF/schemas/wfs/3.1.0/wfs_configuration.xsd" );
    }

    public URL getConfigTemplate() {
        return WFSProvider.class.getResource( "/META-INF/schemas/wfs/3.1.0/example.xml" );
    }

    public ImplementationMetadata<WFSRequestType> getImplementationMetadata() {
        return IMPLEMENTATION_METADATA;
    }

    public OWS<WFSRequestType> getService() {
        return new WFSController();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ResourceManager>[] getDependencies() {
        return new Class[] { FeatureStoreManager.class };
    }

}