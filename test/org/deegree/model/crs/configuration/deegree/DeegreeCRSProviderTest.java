//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.deegree.model.crs.configuration.deegree;

import junit.framework.TestCase;

import org.deegree.model.crs.CRSCodeType;
import org.deegree.model.crs.components.Axis;
import org.deegree.model.crs.components.Ellipsoid;
import org.deegree.model.crs.components.GeodeticDatum;
import org.deegree.model.crs.components.PrimeMeridian;
import org.deegree.model.crs.components.Unit;
import org.deegree.model.crs.configuration.CRSConfiguration;
import org.deegree.model.crs.configuration.CRSProvider;
import org.deegree.model.crs.configuration.deegree.xml.DeegreeCRSProvider;
import org.deegree.model.crs.coordinatesystems.CoordinateSystem;
import org.deegree.model.crs.coordinatesystems.GeographicCRS;
import org.deegree.model.crs.coordinatesystems.ProjectedCRS;
import org.deegree.model.crs.projections.Projection;
import org.deegree.model.crs.projections.cylindric.TransverseMercator;
import org.deegree.model.crs.transformations.helmert.Helmert;
import org.junit.Test;

/**
 * <code>DeegreeCRSProviderTest</code> test the loading of a projected crs as well as the loading of the default
 * configuration.
 * 
 * @author <a href="mailto:bezema@lat-lon.de">Rutger Bezema</a>
 * 
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 * 
 */
public class DeegreeCRSProviderTest extends TestCase {

    /**
     * Tries to load the configuration
     */
	@Test
    public void testLoadingConfiguration() {
        CRSProvider provider = CRSConfiguration.getCRSConfiguration().getProvider();
        assertNotNull( provider );
        assertTrue( provider instanceof DeegreeCRSProvider );
        DeegreeCRSProvider dProvider = (DeegreeCRSProvider) provider;
        assertTrue( dProvider.canExport() );
    }

    /**
     * Tries to create a crs by id.
     */
    @Test
    public void testCRSByID() {
        CRSProvider provider = CRSConfiguration.getCRSConfiguration().getProvider();
        assertNotNull( provider );
        assertTrue( provider instanceof DeegreeCRSProvider );
        DeegreeCRSProvider dProvider = (DeegreeCRSProvider) provider;
        // try loading the gaus krueger zone 2.
        CoordinateSystem testCRS = dProvider.getCRSByCode( new CRSCodeType( "31466", "EPSG" ) );
        assertNotNull( testCRS );
        assertTrue( testCRS instanceof ProjectedCRS );
        ProjectedCRS realCRS = (ProjectedCRS) testCRS;
        assertNotNull( realCRS.getProjection() );
        Projection projection = realCRS.getProjection();
        assertTrue( projection instanceof TransverseMercator );
        // do stuff with projection
        TransverseMercator proj = (TransverseMercator) projection;
        assertEquals( 0.0, proj.getProjectionLatitude() );
        assertEquals( Math.toRadians( 6.0 ), proj.getProjectionLongitude() );
        assertEquals( 1.0, proj.getScale() );
        assertEquals( 2500000.0, proj.getFalseEasting() );
        assertEquals( 0.0, proj.getFalseNorthing() );
        assertTrue( proj.getHemisphere() );

        // test the datum.
        GeodeticDatum datum = realCRS.getGeodeticDatum();
        assertNotNull( datum );
        assertEquals( new CRSCodeType( "6314", "EPSG" ), datum.getCode() );
        assertEquals( PrimeMeridian.GREENWICH, datum.getPrimeMeridian() );

        // test the ellips
        Ellipsoid ellips = datum.getEllipsoid();
        assertNotNull( ellips );
        assertEquals( new CRSCodeType( "7004", "EPSG" ), ellips.getCode() );
        assertEquals( Unit.METRE, ellips.getUnits() );
        assertEquals( 6377397.155, ellips.getSemiMajorAxis() );
        assertEquals( 299.1528128, ellips.getInverseFlattening() );

        // test towgs84 params
        Helmert toWGS = datum.getWGS84Conversion();
        assertNotNull( toWGS );
        assertTrue( toWGS.hasValues() );
        assertEquals( new CRSCodeType( "1777", "EPSG" ), toWGS.getCode() );
        assertEquals( 598.1, toWGS.dx );
        assertEquals( 73.7, toWGS.dy );
        assertEquals( 418.2, toWGS.dz );
        assertEquals( 0.202, toWGS.ex );
        assertEquals( 0.045, toWGS.ey );
        assertEquals( -2.455, toWGS.ez );
        assertEquals( 6.7, toWGS.ppm );

        // test the geographic
        GeographicCRS geographic = realCRS.getGeographicCRS();
        assertNotNull( geographic );
        assertEquals( new CRSCodeType( "4314", "EPSG" ), geographic.getCode() );
        Axis[] ax = geographic.getAxis();
        assertEquals( 2, ax.length );
        assertEquals( Axis.AO_EAST, ax[0].getOrientation() );
        assertEquals( Unit.DEGREE, ax[0].getUnits() );
        assertEquals( Axis.AO_NORTH, ax[1].getOrientation() );
        assertEquals( Unit.DEGREE, ax[1].getUnits() );

        testCRS = dProvider.getCRSByCode( new CRSCodeType( "SOME_DUMMY_CODE", "" ) );
        assertTrue( testCRS == null );

    }
}
