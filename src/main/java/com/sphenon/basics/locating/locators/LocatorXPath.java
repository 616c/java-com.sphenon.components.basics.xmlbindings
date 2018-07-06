package com.sphenon.basics.locating.locators;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.*;
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.encoding.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.returncodes.*;

import com.sphenon.basics.xml.*;
import com.sphenon.basics.xml.returncodes.*;
import com.sphenon.basics.xmlbindings.*;

import org.apache.xerces.xni.parser.XMLInputSource;

import java.io.File;
import java.io.InputStream;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

public class LocatorXPath extends Locator {
    static protected Configuration config;
    static { config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.locating.locators.LocatorXPath"); };
    static {
        XMLBindingsPackageInitialiser.initialise();
    }

    public LocatorXPath (CallContext context, String text_locator_value, Locator sub_locator, String locator_class_parameter_string) {
        super(context, text_locator_value, sub_locator, locator_class_parameter_string);
    }

    /* Parser States -------------------------------------------------------------------- */

    protected LocatorParserState[] getParserStates(CallContext context) {
        CustomaryContext.create((Context)context).throwPreConditionViolation(context, "LocatorXPath text locator value is not interpreted by this locator, therefore there are no parser states available");
        throw (ExceptionPreConditionViolation) null; // compiler insists
    }

    protected boolean canGetParserStates(CallContext context) {
        return false;
    }

    /* Base Acceptors ------------------------------------------------------------------- */

    static protected Vector<LocatorBaseAcceptor> locator_base_acceptors;

    static public class LocatorBaseAcceptor_File extends LocatorBaseAcceptor {
        public LocatorBaseAcceptor_File (CallContext context) {
            super(context, File.class);
        }
        public Object tryAccept(CallContext context, Object base_object_candidate) {
            return (((File) base_object_candidate).isDirectory() == false) ? base_object_candidate : null;
        }
    }

    static protected Vector<LocatorBaseAcceptor> initBaseAcceptors(CallContext context) {
        if (locator_base_acceptors == null) {
            locator_base_acceptors = new Vector<LocatorBaseAcceptor>();
            locator_base_acceptors.add(new LocatorBaseAcceptor_File(context));
            locator_base_acceptors.add(new LocatorBaseAcceptor(context, InputStream.class));
            locator_base_acceptors.add(new LocatorBaseAcceptor(context, XMLNode.class));
            locator_base_acceptors.add(new LocatorBaseAcceptor(context, TreeLeaf.class));
            locator_base_acceptors.add(new LocatorBaseAcceptor(context, Data_MediaObject.class));
        }
        return locator_base_acceptors;
    }

    protected Vector<LocatorBaseAcceptor> getBaseAcceptors(CallContext context) {
        return initBaseAcceptors(context);
    }

    static public void addBaseAcceptor(CallContext context, LocatorBaseAcceptor base_acceptor) {
        initBaseAcceptors(context).add(base_acceptor);
    }

    static protected LocatorClassParameter[] locator_class_parameters;

    protected LocatorClassParameter[] getLocatorClassParameters(CallContext context) {
        if (locator_class_parameters == null) {
            locator_class_parameters = new LocatorClassParameter[] {
                new LocatorClassParameter(context, "targettype", "XMLNode|Text", "XMLNode")
            };
        }
        return locator_class_parameters;
    }
    
    /* ---------------------------------------------------------------------------------- */

    public String getTargetVariableName(CallContext context) {
        return "xml_node";
    }

    static protected RegularExpression nsre = new RegularExpression("^xmlns:([a-z0-9]+)=([^/]+)/(.*)$");

    protected Object retrieveLocalTarget(CallContext context) throws InvalidLocator {
        Object base = lookupBaseObject(context, true);

        XMLNode xn = null;
        try {
            if (base instanceof File) {
                xn = XMLNode.createXMLNode(context, (File) base);
            } else if (base instanceof InputStream) {
                xn = XMLNode.createXMLNode(context, (InputStream) base, "");
            } else if (base instanceof XMLNode) {
                xn = (XMLNode) base;
            } else if (base instanceof TreeLeaf) {
                Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) base).getContent(context))).getData(context)));
                xn = XMLNode.createXMLNode(context, data instanceof Data_MediaObject_File ? new XMLInputSource(null, ((Data_MediaObject_File)(data)).getCurrentFile(context).getPath(), null) : new XMLInputSource(null, null, null, data.getStream(context), null), data.getDispositionFilename(context));
            } else if (base instanceof Data_MediaObject) {
                Data_MediaObject data = (Data_MediaObject) base;
                xn = XMLNode.createXMLNode(context, data instanceof Data_MediaObject_File ? new XMLInputSource(null, ((Data_MediaObject_File)(data)).getCurrentFile(context).getPath(), null) : new XMLInputSource(null, null, null, data.getStream(context), null), data.getDispositionFilename(context));
            } else {
                assert(false);
            }
        } catch (InvalidXML ix) {
            InvalidLocator.createAndThrow(context, ix, "Base '%(base)' for locator '%(locator)' contains invalid XML", "base", base, "locator", this.getTextLocatorValue(context));
            throw (InvalidLocator) null; // compiler insists
        }

        String xpath = this.getTextLocatorValue(context);
        Map<String,String> namespaces = new HashMap<String,String>();
        String[] match;
        while ((match = nsre.tryGetMatches(context, xpath)) != null) {
            namespaces.put(match[0], Encoding.recode(context, match[1], Encoding.URI, Encoding.UTF8));
            xpath = match[2];
        }

        XMLNode xnr = xn.resolveXPath(context, xpath, namespaces);

        String ltype = getLocatorClassParameter(context, "targettype");

        if (ltype.equals("XMLNode")) {
            return xnr;
        } else {
            return xnr.toText(context);
        }
    }
}
