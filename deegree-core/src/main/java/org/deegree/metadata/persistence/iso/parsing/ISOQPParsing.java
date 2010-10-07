//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
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
package org.deegree.metadata.persistence.iso.parsing;

import static org.deegree.protocol.csw.CSWConstants.CSW_202_NS;
import static org.deegree.protocol.csw.CSWConstants.CSW_PREFIX;
import static org.deegree.protocol.csw.CSWConstants.DCT_NS;
import static org.deegree.protocol.csw.CSWConstants.DCT_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.deegree.commons.tom.datetime.Date;
import org.deegree.commons.xml.NamespaceContext;
import org.deegree.commons.xml.XMLAdapter;
import org.deegree.commons.xml.XPath;
import org.deegree.cs.CRS;
import org.deegree.metadata.persistence.MetadataStoreException;
import org.deegree.metadata.persistence.types.BoundingBox;
import org.deegree.metadata.persistence.types.Format;
import org.deegree.metadata.persistence.types.Keyword;
import org.slf4j.Logger;

/**
 * Parsing regarding to ISO and DC application profile. Here the input XML document is parsed into its parts. So this is
 * the entry point to generate a record that fits with the backend. The queryable and returnable properties are
 * disentangled. This is needed to put them into the queryable property tables in the backend and makes them queryable.
 * In this context they are feasible to build the Dublin Core record which has nearly planar elements with no nested
 * areas.
 * 
 * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public final class ISOQPParsing extends XMLAdapter {

    private static final Logger LOG = getLogger( ISOQPParsing.class );

    private static NamespaceContext nsContextISOParsing = new NamespaceContext( XMLAdapter.nsContext );

    private QueryableProperties qp;

    private ReturnableProperties rp;

    // private MetadataValidation mv;

    static {
        nsContextISOParsing.addNamespace( CSW_PREFIX, CSW_202_NS );
        nsContextISOParsing.addNamespace( "srv", "http://www.isotc211.org/2005/srv" );
        nsContextISOParsing.addNamespace( "ows", "http://www.opengis.net/ows" );
        nsContextISOParsing.addNamespace( DCT_PREFIX, DCT_NS );
    }

    public ISOQPParsing() {

    }

    /**
     * Parses the recordelement that should be inserted into the backend. Every elementknot is put into an OMElement and
     * its atomic representation:
     * <p>
     * e.g. the "fileIdentifier" is put into an OMElement identifier and its identification-String is put into the
     * {@link QueryableProperties}.
     * 
     * @param element
     *            the XML element that has to be parsed to be able to generate needed database properties
     * @return {@link ParsedProfileElement}
     * @throws IOException
     */
    public ParsedProfileElement parseAPISO( OMElement element, boolean isUpdate )
                            throws MetadataStoreException {

        OMFactory factory = OMAbstractFactory.getOMFactory();

        setRootElement( element );
        if ( element.getDefaultNamespace() != null ) {
            nsContextISOParsing.addNamespace( rootElement.getDefaultNamespace().getPrefix(),
                                              rootElement.getDefaultNamespace().getNamespaceURI() );
        }

        qp = new QueryableProperties();

        rp = new ReturnableProperties();

        // for ( String error : ca.getMv().validate( rootElement ) ) {
        // throw new MetadataStoreException( "VALIDATION-ERROR: " + error );
        // }

        /*---------------------------------------------------------------
         * 
         * 
         * (default) Language
         * 
         * 
         *---------------------------------------------------------------*/
        String language = getNodeAsString(
                                           rootElement,
                                           new XPath(
                                                      "./gmd:language/gco:CharacterString | ./gmd:language/gmd:LanguageCode/@codeListValue",
                                                      nsContextISOParsing ), null );

        // Locale locale = new Locale(
        // getNodeAsString(
        // rootElement,
        // new XPath(
        // "./gmd:language/gco:CharacterString | ./gmd:language/gmd:LanguageCode/@codeListValue",
        // nsContextISOParsing ), null ) );

        qp.setLanguage( language );
        // LOG.debug( getElement( rootElement, new XPath( "./gmd:language", nsContextISOParsing ) ).toString() );

        /*---------------------------------------------------------------
         * 
         * 
         * CharacterSet
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * ParentIdentifier
         * 
         * 
         *---------------------------------------------------------------*/
        String parentId = getNodeAsString( rootElement, new XPath( "./gmd:parentIdentifier/gco:CharacterString",
                                                                   nsContextISOParsing ), null );
        qp.setParentIdentifier( parentId );

        /*---------------------------------------------------------------
         * 
         * 
         * Type
         * HierarchieLevel
         * 
         *---------------------------------------------------------------*/
        /**
         * if provided data is a dataset: type = dataset (default)
         * <p>
         * if provided data is a datasetCollection: type = series
         * <p>
         * if provided data is an application: type = application
         * <p>
         * if provided data is a service: type = service
         */
        String type = getNodeAsString( rootElement, new XPath( "./gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue",
                                                               nsContextISOParsing ), "dataset" );
        qp.setType( type );

        /*---------------------------------------------------------------
         * 
         * 
         * HierarchieLevelName
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * Contact
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * DateStamp
         * Modified
         * 
         * 
         *---------------------------------------------------------------*/
        String[] dateString = getNodesAsStrings( rootElement, new XPath( "./gmd:dateStamp/gco:Date",
                                                                         nsContextISOParsing ) );
        Date[] date = new Date[dateString.length];
        try {
            int counter = 0;
            if ( dateString != null ) {
                for ( String dates : dateString ) {

                    date[counter++] = new Date( dates );

                }
            }
        } catch ( ParseException e ) {

            LOG.debug( "error: " + e.getMessage(), e );
            throw new MetadataStoreException( e.getMessage() );
        }

        qp.setModified( date );

        /*---------------------------------------------------------------
         * 
         * 
         * MetadataStandardName
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * MetadataStandardVersion
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * DataSetURI
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * Locale (for multilinguarity)
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * SpatialRepresentationInfo
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * ReferenceSystemInfo
         * 
         * 
         *---------------------------------------------------------------*/
        List<OMElement> crsElements = getElements(
                                                   rootElement,
                                                   new XPath(
                                                              "./gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier",
                                                              nsContextISOParsing ) );

        List<CRS> crsList = new ArrayList<CRS>();
        for ( OMElement crsElement : crsElements ) {
            String crsIdentification = getNodeAsString( crsElement, new XPath( "./gmd:code/gco:CharacterString",
                                                                               nsContextISOParsing ), "" );

            // String crsAuthority = getNodeAsString( crsElement, new XPath( "./gmd:codeSpace/gco:CharacterString",
            // nsContextISOParsing ), "" );
            //
            // String crsVersion = getNodeAsString( crsElement,
            // new XPath( "./gmd:version/gco:CharacterString", nsContextISOParsing ), "" );

            CRS crs = new CRS( crsIdentification );

            crsList.add( crs );
        }

        /*---------------------------------------------------------------
         * 
         * 
         * MetadataExtensionInfo
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * IdentificationInfo
         * 
         * 
         *---------------------------------------------------------------*/
        List<OMElement> identificationInfo = getElements( rootElement, new XPath( "./gmd:identificationInfo",
                                                                                  nsContextISOParsing ) );

        ParseIdentificationInfo pI = new ParseIdentificationInfo( factory, nsContextISOParsing );
        pI.parseIdentificationInfo( identificationInfo, qp, rp, crsList );
        /*---------------------------------------------------------------
         * 
         * 
         * FileIdentifier
         * 
         * 
         *---------------------------------------------------------------*/
        String[] fileIdentifierString = getNodesAsStrings( rootElement,
                                                           new XPath( "./gmd:fileIdentifier/gco:CharacterString",
                                                                      nsContextISOParsing ) );
        // List<String> idList = ca.getFi().determineFileIdentifier( fileIdentifierString,
        // pI.getResourceIdentifierList(),
        // pI.getDataIdentificationId(),
        // pI.getDataIdentificationUuId(), isUpdate );

        qp.setIdentifier( fileIdentifierString );

        // TODO

        /*---------------------------------------------------------------
         * 
         * 
         * ContentInfo
         * 
         * 
         *---------------------------------------------------------------*/

        parseDistributionInfo();

        parseDataQualityInfo();

        /*---------------------------------------------------------------
         * 
         * 
         * PortrayalCatalogueInfo
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * MetadataConstraints
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * ApplicationSchemaInfo
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * MetadataMaintenance
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * Series
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * Describes
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * PropertyType
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * FeatureType
         * 
         * 
         *---------------------------------------------------------------*/

        /*---------------------------------------------------------------
         * 
         * 
         * FeatureAttribute
         * 
         * 
         *---------------------------------------------------------------*/

        /*
         * sets the properties that are needed for building DC records
         */

        return new ParsedProfileElement( qp, rp );

    }

    /**
     * DistributionInfo
     */
    private void parseDistributionInfo() {

        List<OMElement> formats = new ArrayList<OMElement>();

        formats.addAll( getElements(
                                     rootElement,
                                     new XPath(
                                                "./gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorFormat/gmd:MD_Format",
                                                nsContextISOParsing ) ) );

        formats.addAll( getElements(
                                     rootElement,
                                     new XPath(
                                                "./gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format",
                                                nsContextISOParsing ) ) );

        // String onlineResource = getNodeAsString(
        // rootElement,
        // new XPath(
        // "./gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource/gmd:linkage/gmd:URL",
        // nsContextISOParsing ), null );

        List<Format> listOfFormats = new ArrayList<Format>();
        for ( OMElement md_format : formats ) {

            String formatName = getNodeAsString( md_format, new XPath( "./gmd:name/gco:CharacterString",
                                                                       nsContextISOParsing ), null );

            String formatVersion = getNodeAsString( md_format, new XPath( "./gmd:version/gco:CharacterString",
                                                                          nsContextISOParsing ), null );

            Format formatClass = new Format( formatName, formatVersion );
            listOfFormats.add( formatClass );

        }

        qp.setFormat( listOfFormats );

    }

    /**
     * DataQualityInfo
     */
    private void parseDataQualityInfo() {
        qp.setLineage( getNodeAsString(
                                        rootElement,
                                        new XPath(
                                                   "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString",
                                                   nsContextISOParsing ), "" ) );
        qp.setDegree( getNodeAsBoolean(
                                        rootElement,
                                        new XPath(
                                                   "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:pass/gco:Boolean",
                                                   nsContextISOParsing ), false ) );

        OMElement titleElem = getElement(
                                          rootElement,
                                          new XPath(
                                                     "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:title",
                                                     nsContextISOParsing ) );

        String[] titleList = getNodesAsStrings(
                                                titleElem,
                                                new XPath(
                                                           "./gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString",
                                                           nsContextISOParsing ) );

        List<String> titleStringList = new ArrayList<String>();
        titleStringList.addAll( Arrays.asList( getNodeAsString(
                                                                rootElement,
                                                                new XPath(
                                                                           "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString",
                                                                           nsContextISOParsing ), "" ) ) );
        if ( titleList != null ) {
            titleStringList.addAll( Arrays.asList( titleList ) );
        }
        qp.setSpecificationTitle( titleStringList );

        qp.setSpecificationDateType( getNodeAsString(
                                                      rootElement,
                                                      new XPath(
                                                                 "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue",
                                                                 nsContextISOParsing ), "" ) );

        String specificationDateString = getNodeAsString(
                                                          rootElement,
                                                          new XPath(
                                                                     "./gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/gmd:DQ_DomainConsistency/gmd:result/gmd:DQ_ConformanceResult/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date",
                                                                     nsContextISOParsing ), null );
        Date dateSpecificationDate = null;

        try {
            if ( dateSpecificationDate != null ) {
                dateSpecificationDate = new Date( specificationDateString );
            }
        } catch ( ParseException e ) {

            LOG.debug( "error: " + e.getMessage(), e );

        }

        qp.setSpecificationDate( dateSpecificationDate );

    }

    /**
     * This method parses the OMElement regarding to the Dublin Core profile.
     * 
     * @param element
     * @return {@link ParsedProfileElement}
     * 
     * @throws IOException
     * @throws MetadataStoreException
     */
    public ParsedProfileElement parseAPDC( OMElement element )
                            throws MetadataStoreException {

        qp = new QueryableProperties();

        rp = new ReturnableProperties();

        setRootElement( element );
        if ( element.getDefaultNamespace() != null ) {
            nsContextISOParsing.addNamespace( rootElement.getDefaultNamespace().getPrefix(),
                                              rootElement.getDefaultNamespace().getNamespaceURI() );
        }

        // for ( String error : ca.getMv().validate( rootElement ) ) {
        // throw new MetadataStoreException( "VALIDATION-ERROR: " + error );
        // }

        List<Keyword> keywordList = new ArrayList<Keyword>();

        List<Format> formatList = new ArrayList<Format>();
        // TODO anyText
        // StringWriter anyText = new StringWriter();

        String[] b = getNodesAsStrings( rootElement, new XPath( "./dc:identifier", nsContextISOParsing ) );
        qp.setIdentifier( b );

        rp.setCreator( getNodeAsString( rootElement, new XPath( "./dc:creator", nsContextISOParsing ), null ) );

        Keyword keyword = new Keyword( null, Arrays.asList( getNodesAsStrings( rootElement,
                                                                               new XPath( "./dc:subject",
                                                                                          nsContextISOParsing ) ) ),
                                       null );
        keywordList.add( keyword );
        qp.setKeywords( keywordList );

        qp.setTitle( Arrays.asList( getNodesAsStrings( rootElement, new XPath( "./dc:title", nsContextISOParsing ) ) ) );

        // List<String> abstractList = new ArrayList<String>();
        // TODO because there are more abstracts possible in theory...
        // abstractList.add( e )

        qp.set_abstract( Arrays.asList( getNodesAsStrings( rootElement, new XPath( "./dct:abstract",
                                                                                   nsContextISOParsing ) ) ) );

        String[] formatStrings = getNodesAsStrings( rootElement, new XPath( "./dc:format", nsContextISOParsing ) );

        for ( String s : formatStrings ) {

            Format format = new Format( s, null );
            formatList.add( format );
        }

        qp.setFormat( formatList );

        // Date[] modified = null;
        // try {
        // modified = new Date(
        // getNodeAsString( rootElement, new XPath( "./dct:modified", nsContextISOParsing ), null ) );
        // } catch ( ParseException e ) {
        //
        // LOG.debug( "Error while parsing the date: {} ", e.getMessage() );
        // throw new MetadataStoreException( "Error while parsing the date: {} ", e );
        // }
        // qp.setModified( modified );

        qp.setType( getNodeAsString( rootElement, new XPath( "./dc:type", nsContextISOParsing ), null ) );

        String bbox_lowerCorner = getNodeAsString(
                                                   rootElement,
                                                   new XPath(
                                                              "./ows:BoundingBox/ows:LowerCorner | ./ows:WGS84BoundingBox/ows:LowerCorner",
                                                              nsContextISOParsing ), null );
        String bbox_upperCorner = getNodeAsString(
                                                   rootElement,
                                                   new XPath(
                                                              "./ows:BoundingBox/ows:UpperCorner | ./ows:WGS84BoundingBox/ows:UpperCorner",
                                                              nsContextISOParsing ), null );

        if ( bbox_lowerCorner != null && bbox_upperCorner != null ) {
            String[] lowerCornerSplitting = bbox_lowerCorner.split( " " );
            String[] upperCornerSplitting = bbox_upperCorner.split( " " );

            double boundingBoxWestLongitude = Double.parseDouble( lowerCornerSplitting[0] );

            double boundingBoxEastLongitude = Double.parseDouble( lowerCornerSplitting[1] );

            double boundingBoxSouthLatitude = Double.parseDouble( upperCornerSplitting[0] );

            double boundingBoxNorthLatitude = Double.parseDouble( upperCornerSplitting[1] );

            qp.setBoundingBox( new BoundingBox( boundingBoxWestLongitude, boundingBoxSouthLatitude,
                                                boundingBoxEastLongitude, boundingBoxNorthLatitude ) );
        }
        rp.setPublisher( getNodeAsString( rootElement, new XPath( "./dc:publisher", nsContextISOParsing ), null ) );

        rp.setContributor( getNodeAsString( rootElement, new XPath( "./dc:contributor", nsContextISOParsing ), null ) );

        rp.setSource( getNodeAsString( rootElement, new XPath( "./dc:source", nsContextISOParsing ), null ) );

        return new ParsedProfileElement( qp, rp );

    }

}
