//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/deegree3/commons/trunk/src/org/deegree/model/feature/Feature.java $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2012 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

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
package org.deegree.gml.commons;

import static java.util.Collections.EMPTY_LIST;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_ELEMENT;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_EMPTY;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_MIXED;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import static org.deegree.commons.tom.primitive.BaseType.BOOLEAN;
import static org.deegree.commons.xml.CommonNamespaces.XLNNS;
import static org.deegree.commons.xml.CommonNamespaces.XSINS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.deegree.commons.tom.ElementNode;
import org.deegree.commons.tom.TypedObjectNode;
import org.deegree.commons.tom.array.TypedObjectNodeArray;
import org.deegree.commons.tom.genericxml.GenericXMLElement;
import org.deegree.commons.tom.gml.GMLObjectCategory;
import org.deegree.commons.tom.gml.GMLReferenceResolver;
import org.deegree.commons.tom.gml.property.Property;
import org.deegree.commons.tom.gml.property.PropertyType;
import org.deegree.commons.tom.ows.CodeType;
import org.deegree.commons.tom.ows.StringOrRef;
import org.deegree.commons.tom.primitive.PrimitiveType;
import org.deegree.commons.tom.primitive.PrimitiveValue;
import org.deegree.commons.uom.Measure;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.commons.xml.XMLAdapter;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.commons.xml.stax.XMLStreamReaderWrapper;
import org.deegree.commons.xml.stax.XMLStreamUtils;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.feature.Feature;
import org.deegree.feature.i18n.Messages;
import org.deegree.feature.property.GenericProperty;
import org.deegree.feature.property.SimpleProperty;
import org.deegree.feature.types.AppSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.property.ArrayPropertyType;
import org.deegree.feature.types.property.CodePropertyType;
import org.deegree.feature.types.property.CustomPropertyType;
import org.deegree.feature.types.property.EnvelopePropertyType;
import org.deegree.feature.types.property.FeaturePropertyType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.GeometryPropertyType.GeometryType;
import org.deegree.feature.types.property.MeasurePropertyType;
import org.deegree.feature.types.property.ObjectPropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.feature.types.property.StringOrRefPropertyType;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.refs.GeometryReference;
import org.deegree.gml.GMLDocumentIdContext;
import org.deegree.gml.GMLStreamReader;
import org.deegree.gml.feature.FeatureReference;
import org.deegree.gml.feature.GMLFeatureReader;
import org.deegree.gml.schema.WellKnownGMLTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each concrete extension is a parser for a specific category of GML objects.
 * 
 * @see GMLObjectCategory
 * @see GMLFeatureReader
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public abstract class AbstractGMLObjectReader extends XMLAdapter {

    private static final Logger LOG = LoggerFactory.getLogger( AbstractGMLObjectReader.class );

    protected final String gmlNs;

    protected final GMLStreamReader gmlStreamReader;

    protected final GMLReferenceResolver specialResolver;

    protected final GMLDocumentIdContext idContext;

    protected static final QName XSI_NIL = new QName( XSINS, "nil", "xsi" );

    // TODO should be final, but is currently modified by GMLFeatureReader
    protected AppSchema schema;

    /**
     * Creates a new {@link AbstractGMLObjectReader} instance.
     * 
     * @param gmlStreamReader
     *            GML version, must not be <code>null</code>
     */
    protected AbstractGMLObjectReader( GMLStreamReader gmlStreamReader ) {
        this.gmlStreamReader = gmlStreamReader;
        this.specialResolver = gmlStreamReader.getResolver();
        this.idContext = gmlStreamReader.getIdContext();
        // TODO
        this.schema = gmlStreamReader.getAppSchema();
        this.gmlNs = gmlStreamReader.getVersion().getNamespace();
    }

    /**
     * Returns the object representation for the given property element.
     * 
     * @param xmlStream
     *            cursor must point at the <code>START_ELEMENT</code> event of the property, afterwards points at the
     *            next event after the <code>END_ELEMENT</code> of the property
     * @param propDecl
     *            property declaration
     * @param crs
     *            default SRS for all a descendant geometry properties
     * @return object representation for the given property element.
     * @throws XMLParsingException
     * @throws XMLStreamException
     * @throws UnknownCRSException
     */
    public Property parseProperty( XMLStreamReaderWrapper xmlStream, PropertyType propDecl, ICRS crs )
                            throws XMLParsingException, XMLStreamException, UnknownCRSException {

        if ( LOG.isDebugEnabled() ) {
            LOG.debug( "- parsing property (begin): " + xmlStream.getCurrentEventInfo() );
            LOG.debug( "- property declaration: " + propDecl );
        }

        Property property = null;
        if ( propDecl instanceof SimplePropertyType ) {
            property = parseSimpleProperty( xmlStream, (SimplePropertyType) propDecl );
        } else if ( propDecl instanceof GeometryPropertyType ) {
            property = parseGeometryProperty( xmlStream, (GeometryPropertyType) propDecl, crs );
        } else if ( propDecl instanceof FeaturePropertyType ) {
            property = parseFeatureProperty( xmlStream, (FeaturePropertyType) propDecl, crs );
        } else if ( propDecl instanceof CustomPropertyType ) {
            property = parseCustomProperty( xmlStream, (CustomPropertyType) propDecl, crs );
        } else if ( propDecl instanceof EnvelopePropertyType ) {
            property = parseEnvelopeProperty( xmlStream, (EnvelopePropertyType) propDecl, crs );
        } else if ( propDecl instanceof CodePropertyType ) {
            property = parseCodeProperty( xmlStream, (CodePropertyType) propDecl );
        } else if ( propDecl instanceof MeasurePropertyType ) {
            property = parseMeasureProperty( xmlStream, (MeasurePropertyType) propDecl );
        } else if ( propDecl instanceof StringOrRefPropertyType ) {
            property = parseStringOrRefProperty( xmlStream, (StringOrRefPropertyType) propDecl );
        } else if ( propDecl instanceof ArrayPropertyType ) {
            property = parseArrayProperty( xmlStream, (ArrayPropertyType) propDecl, crs );
        } else {
            throw new RuntimeException( "Internal error in GMLFeatureReader: property type " + propDecl.getClass()
                                        + " not handled." );
        }

        if ( LOG.isDebugEnabled() ) {
            LOG.debug( " - parsing property (end): " + xmlStream.getCurrentEventInfo() );
        }
        return property;
    }

    public final PropertyType findConcretePropertyType( final QName elemName, final PropertyType pt ) {
        // LOG.debug( "Checking if '" + elemName + "' is a valid substitution for '" + pt.getName() + "'" );

        for ( final PropertyType substitutionPt : pt.getSubstitutions() ) {
            // TODO !substitutionPt.isAbstract()
            if ( elemName.equals( substitutionPt.getName() ) ) {
                // LOG.debug( "Yep. Substitutable for '" + substitutionPt.getName() + "'" );
                return substitutionPt;
            }
        }
        // LOG.debug( "Nope." );
        return null;
    }

    private Property parseSimpleProperty( XMLStreamReaderWrapper xmlStream, SimplePropertyType propDecl )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        Property property = null;
        if ( attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue() ) {
            property = new GenericProperty( propDecl, propName, null, true );
            // TODO need to check that element is indeed empty?
            XMLStreamUtils.nextElement( xmlStream );
        } else {
            property = createSimpleProperty( xmlStream, propDecl, xmlStream.getElementText().trim() );
        }
        return property;
    }

    @SuppressWarnings("unchecked")
    private Property parseFeatureProperty( XMLStreamReaderWrapper xmlStream, FeaturePropertyType propDecl, ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        Property property = null;

        String href = xmlStream.getAttributeValue( XLNNS, "href" );
        if ( href != null ) {
            FeatureReference refFeature = null;
            if ( specialResolver != null ) {
                refFeature = new FeatureReference( specialResolver, href, xmlStream.getSystemId() );
            } else {
                refFeature = new FeatureReference( idContext, href, xmlStream.getSystemId() );
            }
            idContext.addReference( refFeature );
            List<TypedObjectNode> values = new ArrayList<TypedObjectNode>();
            values.add( refFeature );
            property = new GenericProperty( propDecl, propName, refFeature, attrs, values );
            XMLStreamUtils.skipElement( xmlStream );
        } else {
            // inline feature
            if ( xmlStream.nextTag() == START_ELEMENT ) {
                // TODO make this check (no constraints on contained feature
                // type) better
                if ( propDecl.getFTName() != null ) {
                    FeatureType expectedFt = propDecl.getValueFt();
                    FeatureType presentFt = lookupFeatureType( xmlStream, xmlStream.getName(), true );
                    if ( !schema.isSubType( expectedFt, presentFt ) ) {
                        String msg = Messages.getMessage( "ERROR_PROPERTY_WRONG_FEATURE_TYPE", expectedFt.getName(),
                                                          propName, presentFt.getName() );
                        throw new XMLParsingException( xmlStream, msg );
                    }
                }
                Feature subFeature = gmlStreamReader.getFeatureReader().parseFeature( xmlStream, crs );
                List<TypedObjectNode> values = new ArrayList<TypedObjectNode>();
                values.add( subFeature );
                property = new GenericProperty( propDecl, propName, subFeature, attrs, values );
                xmlStream.skipElement();
            } else {
                // yes, empty feature property elements are actually valid
                property = new GenericProperty( propDecl, propName, null, attrs, EMPTY_LIST );
            }
        }
        return property;
    }

    private Property parseGeometryProperty( XMLStreamReaderWrapper xmlStream, GeometryPropertyType propDecl, ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        Property property = null;

        String href = xmlStream.getAttributeValue( CommonNamespaces.XLNNS, "href" );
        if ( href != null ) {
            GeometryReference<Geometry> refGeometry = null;
            // TODO respect allowed geometry types (Point, Surface, etc.)
            if ( specialResolver != null ) {
                refGeometry = new GeometryReference<Geometry>( specialResolver, href, xmlStream.getSystemId() );
            } else {
                refGeometry = new GeometryReference<Geometry>( idContext, href, xmlStream.getSystemId() );
            }
            idContext.addReference( refGeometry );
            property = new GenericProperty( propDecl, propName, refGeometry, isNilled );
            XMLStreamUtils.skipElement( xmlStream );
        } else {
            if ( xmlStream.nextTag() == START_ELEMENT ) {
                Geometry geometry = gmlStreamReader.getGeometryReader().parse( xmlStream, crs );
                boolean compatible = false;
                for ( GeometryType allowedType : propDecl.getAllowedGeometryTypes() ) {
                    if ( allowedType.isCompatible( geometry ) ) {
                        compatible = true;
                        break;
                    }
                }
                // check required for wfs-1.1.0-Transaction-tc10.1.2 (maybe move it to feature store level)
                if ( !compatible ) {
                    String msg = "Value for geometry property is invalid. Specified geometry value "
                                 + geometry.getClass() + " is not allowed here. Allowed geometries are: "
                                 + propDecl.getAllowedGeometryTypes();
                    throw new XMLParsingException( xmlStream, msg );
                }
                property = new GenericProperty( propDecl, propName, geometry, isNilled );
                xmlStream.nextTag();
            } else {
                // yes, empty geometry property elements are actually valid
                property = new GenericProperty( propDecl, propName, null, isNilled );
            }
        }
        return property;
    }

    private Property parseEnvelopeProperty( XMLStreamReaderWrapper xmlStream, EnvelopePropertyType propDecl, ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        Property property = null;
        Envelope env = null;
        xmlStream.nextTag();
        if ( xmlStream.getName().equals( new QName( gmlNs, "Null" ) ) ) {
            // TODO extract
            XMLStreamUtils.skipElement( xmlStream );
        } else if ( xmlStream.getName().equals( new QName( gmlNs, "null" ) ) ) {
            // GML 2 uses "null" instead of "Null"
            // TODO
            XMLStreamUtils.skipElement( xmlStream );
        } else {
            env = gmlStreamReader.getGeometryReader().parseEnvelope( xmlStream, crs );
            property = new GenericProperty( propDecl, propName, env, isNilled );
        }
        xmlStream.nextTag();
        return property;
    }

    private Property parseCodeProperty( XMLStreamReaderWrapper xmlStream, CodePropertyType propDecl )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        String codeSpace = xmlStream.getAttributeValue( null, "codeSpace" );
        String code = xmlStream.getElementText().trim();
        CodeType value = new CodeType( code, codeSpace );
        return new GenericProperty( propDecl, propName, value, isNilled );
    }

    private Property parseMeasureProperty( XMLStreamReaderWrapper xmlStream, MeasurePropertyType propDecl )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        String uom = xmlStream.getAttributeValue( null, "uom" );
        Measure value = new Measure( xmlStream.getElementText(), uom );
        return new GenericProperty( propDecl, propName, value, isNilled );
    }

    private Property parseStringOrRefProperty( XMLStreamReaderWrapper xmlStream, StringOrRefPropertyType propDecl )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        String ref = xmlStream.getAttributeValue( CommonNamespaces.XLNNS, "href" );
        String string = xmlStream.getElementText().trim();
        return new GenericProperty( propDecl, propName, new StringOrRef( string, ref ), isNilled );
    }

    private Property parseArrayProperty( XMLStreamReaderWrapper xmlStream, ArrayPropertyType propDecl, ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        List<Feature> elems = new ArrayList<Feature>();
        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        boolean isNilled = attrs.containsKey( XSI_NIL ) && (Boolean) attrs.get( XSI_NIL ).getValue();
        XMLStreamUtils.nextElement( xmlStream );
        while ( !xmlStream.isEndElement() ) {
            Feature elem = gmlStreamReader.getFeatureReader().parseFeature( xmlStream, crs );
            elems.add( elem );
            XMLStreamUtils.nextElement( xmlStream );
        }
        Feature[] elemArray = elems.toArray( new Feature[elems.size()] );
        TypedObjectNodeArray<Feature> value = new TypedObjectNodeArray<Feature>( elemArray );
        return new GenericProperty( propDecl, propName, value, isNilled );
    }

    private Property parseCustomProperty( XMLStreamReaderWrapper xmlStream, CustomPropertyType propDecl, ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        QName propName = xmlStream.getName();
        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, propDecl.getElementDecl() );
        TypedObjectNode ton = parseComplexXMLElement( xmlStream, propDecl.getElementDecl(), crs );
        List<TypedObjectNode> children = null;
        XSElementDeclaration elDecl = null;
        if ( ton instanceof ElementNode ) {
            GenericXMLElement xmlEl = (GenericXMLElement) ton;
            children = xmlEl.getChildren();
            return new GenericProperty( propDecl, propName, null, attrs, children, xmlEl.getXSType() );
        }
        // TODO should the property value actually be null?
        return new GenericProperty( propDecl, propName, ton, attrs, children, elDecl );
    }

    /**
     * <ul>
     * <li>Precondition: cursor must point at <code>START_ELEMENT</code> event</li>
     * <li>Postcondition: cursor points at the corresponding <code>END_ELEMENT</code> event</li>
     * </ul>
     */
    private TypedObjectNode parseGenericXMLElement( XMLStreamReaderWrapper xmlStream, XSElementDeclaration elDecl,
                                                    ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        QName startElName = xmlStream.getName();

        TypedObjectNode node = null;
        XSTypeDefinition xsdValueType = elDecl.getTypeDefinition();
        if ( xsdValueType.getTypeCategory() == SIMPLE_TYPE ) {
            node = parseSimpleXMLElement( xmlStream, elDecl );
        } else {
            ObjectPropertyType propDecl = schema.getCustomElDecl( elDecl );
            if ( propDecl != null ) {
                if ( propDecl instanceof GeometryPropertyType ) {
                    node = parseGeometryProperty( xmlStream, (GeometryPropertyType) propDecl, crs );
                } else if ( propDecl instanceof FeaturePropertyType ) {
                    node = parseFeatureProperty( xmlStream, (FeaturePropertyType) propDecl, crs );
                } else {
                    throw new RuntimeException( "Internal error. Unhandled GML object property type "
                                                + propDecl.getClass().getName() );
                }
            } else {
                node = parseComplexXMLElement( xmlStream, elDecl, crs );
            }
        }
        xmlStream.require( END_ELEMENT, startElName.getNamespaceURI(), startElName.getLocalPart() );
        return node;
    }

    private GenericXMLElement parseSimpleXMLElement( XMLStreamReaderWrapper xmlStream, XSElementDeclaration elDecl )
                            throws XMLStreamException {
        XSSimpleTypeDefinition xsType = (XSSimpleTypeDefinition) elDecl.getTypeDefinition();
        TypedObjectNode child = new PrimitiveValue( xmlStream.getElementText(), new PrimitiveType( xsType ) );
        return new GenericXMLElement( xmlStream.getName(), elDecl, null, Collections.singletonList( child ) );
    }

    private TypedObjectNode parseComplexXMLElement( XMLStreamReaderWrapper xmlStream, XSElementDeclaration elDecl,
                                                    ICRS crs )
                            throws NoSuchElementException, XMLStreamException, XMLParsingException, UnknownCRSException {

        QName elName = xmlStream.getName();
        LOG.debug( "Parsing complex XML element " + elName );
        if ( gmlStreamReader.getGeometryReader().isGeometryElement( xmlStream ) ) {
            return gmlStreamReader.getGeometryReader().parse( xmlStream );
        } else if ( schema.getFeatureType( elName ) != null ) {
            return gmlStreamReader.getFeatureReader().parseFeature( xmlStream, crs );
        }

        XSComplexTypeDefinition xsdValueType = (XSComplexTypeDefinition) elDecl.getTypeDefinition();

        Map<QName, PrimitiveValue> attrs = parseAttributes( xmlStream, xsdValueType );
        List<TypedObjectNode> children = new ArrayList<TypedObjectNode>();

        Map<QName, XSElementDeclaration> childElementDecls = schema.getAllowedChildElementDecls( xsdValueType );

        switch ( xsdValueType.getContentType() ) {
        case CONTENTTYPE_ELEMENT: {
            // TODO respect order + multiplicity of child elements
            int eventType = 0;
            while ( ( eventType = xmlStream.next() ) != END_ELEMENT ) {
                if ( eventType == START_ELEMENT ) {
                    QName childElName = xmlStream.getName();
                    if ( !childElementDecls.containsKey( childElName ) ) {
                        String msg = "Element '" + childElName + "' is not allowed at this position.";
                        throw new XMLParsingException( xmlStream, msg );
                    }
                    TypedObjectNode child = parseGenericXMLElement( xmlStream, childElementDecls.get( childElName ),
                                                                    crs );
                    // LOG.debug( "adding: " + childElName + ", " + child.getClass().getName() );
                    children.add( child );
                }
            }
            break;
        }
        case CONTENTTYPE_SIMPLE: {
            int eventType = 0;
            while ( ( eventType = xmlStream.next() ) != END_ELEMENT ) {
                if ( eventType == CDATA || eventType == CHARACTERS ) {
                    PrimitiveValue pb = new PrimitiveValue( xmlStream.getText(),
                                                            new PrimitiveType( xsdValueType.getSimpleType() ) );
                    children.add( pb );
                } else if ( eventType == START_ELEMENT ) {
                    QName childElName = xmlStream.getName();
                    if ( !childElementDecls.containsKey( childElName ) ) {
                        String msg = "Element '" + childElName + "' is not allowed at this position.";
                        throw new XMLParsingException( xmlStream, msg );
                    }
                }
            }
            break;
        }
        case CONTENTTYPE_MIXED: {
            int eventType = 0;
            while ( ( eventType = xmlStream.next() ) != END_ELEMENT ) {
                if ( eventType == START_ELEMENT ) {
                    QName childElName = xmlStream.getName();

                    if ( !childElementDecls.containsKey( childElName ) ) {
                        String msg = "Element '" + childElName + "' is not allowed at this position.";
                        throw new XMLParsingException( xmlStream, msg );
                    }
                    TypedObjectNode child = parseGenericXMLElement( xmlStream, childElementDecls.get( childElName ),
                                                                    crs );
                    children.add( child );
                } else if ( eventType == CDATA || eventType == CHARACTERS ) {
                    // mixed content -> use string as primitive type
                    String s = xmlStream.getText();
                    if ( !s.trim().isEmpty() ) {
                        children.add( new PrimitiveValue( s ) );
                    }
                }
            }
            break;
        }
        case CONTENTTYPE_EMPTY: {
            if ( XMLStreamUtils.nextElement( xmlStream ) != END_ELEMENT ) {
                throw new XMLParsingException( xmlStream, "Empty element types don't allow content." );
            }
            break;
        }
        }

        return new GenericXMLElement( xmlStream.getName(), elDecl, attrs, children );
    }

    @Deprecated
    private Map<QName, PrimitiveValue> parseAttributes( XMLStreamReader xmlStream, XSComplexTypeDefinition xsdValueType ) {

        Map<QName, XSAttributeDeclaration> attrDecls = new HashMap<QName, XSAttributeDeclaration>();
        for ( int i = 0; i < xsdValueType.getAttributeUses().getLength(); i++ ) {
            XSAttributeDeclaration attrDecl = ( (XSAttributeUse) xsdValueType.getAttributeUses().item( i ) ).getAttrDeclaration();
            QName name = new QName( attrDecl.getNamespace(), attrDecl.getName() );
            attrDecls.put( name, attrDecl );
        }

        Map<QName, PrimitiveValue> attrs = new LinkedHashMap<QName, PrimitiveValue>();
        for ( int i = 0; i < xmlStream.getAttributeCount(); i++ ) {
            QName name = xmlStream.getAttributeName( i );
            XSAttributeDeclaration attrDecl = attrDecls.get( name );
            if ( attrDecl == null && !XSINS.equals( name.getNamespaceURI() ) ) {
                String msg = "Attribute '" + name + "' is not allowed at this position.";
                throw new XMLParsingException( xmlStream, msg );
            }
            if ( attrDecl != null && !XSINS.equals( name.getNamespaceURI() ) ) {
                String value = xmlStream.getAttributeValue( i );
                // TODO evaluate and check primitive type information
                PrimitiveValue xmlValue = new PrimitiveValue( value, new PrimitiveType( attrDecl.getTypeDefinition() ) );
                attrs.put( name, xmlValue );
            }
        }

        for ( int i = 0; i < xsdValueType.getAttributeUses().getLength(); i++ ) {
            XSAttributeUse attrUse = (XSAttributeUse) xsdValueType.getAttributeUses().item( i );
            if ( attrUse.getRequired() ) {
                XSAttributeDeclaration attrDecl = attrUse.getAttrDeclaration();
                QName name = new QName( attrDecl.getNamespace(), attrDecl.getName() );
                if ( !attrs.containsKey( name ) ) {
                    String msg = "Required attribute '" + name + "' is missing.";
                    throw new XMLParsingException( xmlStream, msg );
                }
            }
        }

        // TODO check if element actually is nillable
        String nilled = xmlStream.getAttributeValue( XSINS, "nil" );
        if ( nilled != null ) {
            PrimitiveValue xmlValue = new PrimitiveValue( nilled, new PrimitiveType( BOOLEAN ) );
            attrs.put( new QName( XSINS, "nil", "xsi" ), xmlValue );
        }
        return attrs;
    }

    /**
     * Parses / validates the attributes for the current START_ELEMENT event.
     * 
     * @param xmlStream
     *            XML stream reader, must point at at START_ELEMENT event (cursor is not moved)
     * @param elDecl
     *            element declaration, can be <code>null</code> (no validation will be performed)
     * @return attributes, never <code>null</code>
     */
    private Map<QName, PrimitiveValue> parseAttributes( XMLStreamReader xmlStream, XSElementDeclaration elDecl ) {

        Map<QName, XSAttributeDeclaration> attrDecls = null;
        if ( elDecl != null && elDecl.getTypeDefinition() instanceof XSComplexTypeDefinition ) {
            XSComplexTypeDefinition xsdValueType = (XSComplexTypeDefinition) elDecl.getTypeDefinition();
            attrDecls = new HashMap<QName, XSAttributeDeclaration>();
            for ( int i = 0; i < xsdValueType.getAttributeUses().getLength(); i++ ) {
                XSAttributeDeclaration attrDecl = ( (XSAttributeUse) xsdValueType.getAttributeUses().item( i ) ).getAttrDeclaration();
                QName name = new QName( attrDecl.getNamespace(), attrDecl.getName() );
                attrDecls.put( name, attrDecl );
            }
        }

        Map<QName, PrimitiveValue> attrs = new LinkedHashMap<QName, PrimitiveValue>();
        for ( int i = 0; i < xmlStream.getAttributeCount(); i++ ) {
            QName name = xmlStream.getAttributeName( i );
            String value = xmlStream.getAttributeValue( i );
            if ( XSI_NIL.equals( name ) ) {
                if ( elDecl != null && !elDecl.getNillable() ) {
                    String msg = "Attribute '" + name + "' is not allowed at this position.";
                    throw new XMLParsingException( xmlStream, msg );
                }
                attrs.put( XSI_NIL, new PrimitiveValue( value, new PrimitiveType( BOOLEAN ) ) );
            } else if ( attrDecls != null ) {
                XSAttributeDeclaration attrDecl = attrDecls.get( name );
                if ( attrDecl == null ) {
                    String msg = "Attribute '" + name + "' is not allowed at this position.";
                    throw new XMLParsingException( xmlStream, msg );
                }
                if ( attrDecl != null ) {
                    // TODO evaluate and check primitive type information
                    PrimitiveValue xmlValue = new PrimitiveValue( value,
                                                                  new PrimitiveType( attrDecl.getTypeDefinition() ) );
                    attrs.put( name, xmlValue );
                }
            } else {
                PrimitiveValue xmlValue = new PrimitiveValue( value );
                attrs.put( name, xmlValue );
            }
        }

        if ( attrDecls != null ) {
            XSComplexTypeDefinition xsdValueType = (XSComplexTypeDefinition) elDecl.getTypeDefinition();
            for ( int i = 0; i < xsdValueType.getAttributeUses().getLength(); i++ ) {
                XSAttributeUse attrUse = (XSAttributeUse) xsdValueType.getAttributeUses().item( i );
                if ( attrUse.getRequired() ) {
                    XSAttributeDeclaration attrDecl = attrUse.getAttrDeclaration();
                    QName name = new QName( attrDecl.getNamespace(), attrDecl.getName() );
                    if ( !attrs.containsKey( name ) ) {
                        String msg = "Required attribute '" + name + "' is missing.";
                        throw new XMLParsingException( xmlStream, msg );
                    }
                }
            }
        }
        return attrs;
    }

    private SimpleProperty createSimpleProperty( XMLStreamReader xmlStream, SimplePropertyType pt, String s )
                            throws XMLParsingException {

        SimpleProperty prop = null;
        try {
            prop = new SimpleProperty( pt, s );
        } catch ( IllegalArgumentException e ) {
            String msg = "Property '" + pt.getName() + "' is not valid: " + e.getMessage();
            throw new XMLParsingException( xmlStream, msg );
        }
        return prop;
    }

    /**
     * Returns the feature type with the given name.
     * <p>
     * If no feature type with the given name is defined, an XMLParsingException is thrown.
     * 
     * @param xmlStreamReader
     * 
     * @param ftName
     *            feature type name to look up
     * @return the feature type with the given name
     * @throws XMLParsingException
     *             if no feature type with the given name is defined
     */
    private FeatureType lookupFeatureType( XMLStreamReaderWrapper xmlStreamReader, QName ftName, boolean exception )
                            throws XMLParsingException {

        FeatureType ft = null;
        ft = schema.getFeatureType( ftName );
        if ( ft == null ) {
            ft = WellKnownGMLTypes.getType( ftName );
        }
        if ( ft == null && exception ) {
            String msg = Messages.getMessage( "ERROR_SCHEMA_FEATURE_TYPE_UNKNOWN", ftName );
            throw new XMLParsingException( xmlStreamReader, msg );
        }
        return ft;
    }
}
