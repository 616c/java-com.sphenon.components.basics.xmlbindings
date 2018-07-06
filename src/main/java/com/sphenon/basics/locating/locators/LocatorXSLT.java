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
import com.sphenon.basics.expression.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.security.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.javaresources.factories.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.returncodes.*;
import com.sphenon.basics.validation.returncodes.*;
import com.sphenon.basics.variatives.*;

import com.sphenon.basics.xml.*;
import com.sphenon.basics.xml.returncodes.*;
import com.sphenon.basics.xmlbindings.*;

import org.apache.xerces.xni.parser.XMLInputSource;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;

import java.util.Vector;
import java.util.regex.*;

public class LocatorXSLT extends Locator {
    static protected long notification_level;
    static public    long adjustNotificationLevel(long new_level) { long old_level = notification_level; notification_level = new_level; return old_level; }
    static public    long getNotificationLevel() { return notification_level; }
    static { notification_level = NotificationLocationContext.getLevel(RootContext.getInitialisationContext(), "com.sphenon.basics.locating.locators.LocatorXSLT"); };

    static protected Configuration config;
    static { config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.locating.locators.LocatorXSLT"); };
    static {
        XMLBindingsPackageInitialiser.initialise();
    }

    public LocatorXSLT (CallContext context, String text_locator_value, Locator sub_locator, String locator_class_parameter_string) {
        super(context, text_locator_value, sub_locator, locator_class_parameter_string);
    }

    /* Parser States -------------------------------------------------------------------- */
    
    static protected LocatorParserState[] locator_parser_states;
        
    protected LocatorParserState[] getParserStates(CallContext context) {
        if (locator_parser_states == null) {
            locator_parser_states = new LocatorParserState[] {
                new LocatorParserState(context, "xsl" , "xsl::Object:1", false, true, null),
                new LocatorParserState(context, null  , ".*::String:1"  , false, true, Object.class)
            };
        }
        return locator_parser_states;
    }

    static protected LocatorClassParameter[] locator_class_parameters;

    protected LocatorClassParameter[] getLocatorClassParameters(CallContext context) {
        if (locator_class_parameters == null) {
            locator_class_parameters = new LocatorClassParameter[] {
                new LocatorClassParameter(context, "targettype", "XMLNode|Stream|Text", "XMLNode")
            };
        }
        return locator_class_parameters;
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
    
    /* ---------------------------------------------------------------------------------- */

    public String getTargetVariableName(CallContext context) {
        return "xml_node";
    }

    static protected class TRes {
        Source source;
        long   last_modification;
    }

    protected OutputStream output_stream;
    public void setOutputStream(CallContext context, OutputStream output_stream) {
        this.output_stream = output_stream;
    }

    protected boolean isTargetCacheable(CallContext context) throws InvalidLocator {
        String ltype = getLocatorClassParameter(context, "targettype");
        boolean cacheable = (ltype.equals("XMLNode") ? true : false);

        if ((this.notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { CustomaryContext.create((Context)context).sendTrace(context, Notifier.SELF_DIAGNOSTICS, "XSLT Locator '%(textlocator)' is %({'not cacheable','cacheable'}[cacheable])", "textlocator", this.text_locator_value, "cacheable", cacheable ? 1 : 0); }

        return cacheable;
    }

    protected Object retrieveLocalTarget(CallContext context) throws InvalidLocator {
        Object base = lookupBaseObject(context, true);

        if ((this.notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { CustomaryContext.create((Context)context).sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Retrieving local target of XSLT Locator '%(textlocator)'...", "textlocator", this.text_locator_value); }

        LocatorStep[] steps = getLocatorSteps(context);

        Locator xsl_locator = steps[0].getLocator(context);
        Object  xsl_source  = xsl_locator.retrieveTarget(context);

        SourceWithTimestamp swt = getStreamSourceFromObject(context, xsl_source);

        // cannot use, this call wants to interpret it
        // swt.getSource(context).setSystemId(xsl_locator.getTextLocator(context));

        Object[] parameters = new Object[(steps.length-1) << 1];

        int index1 = 0;
        int index2 = 1;
        while (index2 < steps.length) {
            parameters[index1] = steps[index2].getAttribute(context);
            index1++;
            parameters[index1] = steps[index2].getValue(context);
            index1++;
            index2++;
        }

        String ltype = getLocatorClassParameter(context, "targettype");

        if (ltype.matches("XMLNode|Text")) {

            XMLNode xn = getXMLNodeFromObject(context, base);

            XMLNode xnr;
            try {
                xnr = xn.transform(context, swt, parameters);
            } catch (TransformationFailure tf) {
                InvalidLocator.createAndThrow(context, tf, "XSLT transformation in locator failed");
                throw (InvalidLocator) null;
            }

            if ((this.notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { CustomaryContext.create((Context)context).sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Returning XSLT Locator XMLNode result..."); }

            if (ltype.equals("XMLNode")) {
                return xnr;
            } else {
                return xnr.toText(context);
            }

        } else {
            SourceWithTimestamp swt2 = getDOMSourceFromObject(context, base);
            Source source = swt2.getSource(context);
            StreamResult result = new StreamResult();
            result.setOutputStream(this.output_stream != null ? this.output_stream : new java.io.ByteArrayOutputStream());

            try {
                XMLUtil.transform(context, source, result, swt, parameters);
            } catch (TransformationFailure tf) {
                InvalidLocator.createAndThrow(context, tf, "XSLT transformation failed");
                throw (InvalidLocator) null;
            }

            if ((this.notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { CustomaryContext.create((Context)context).sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Returning XSLT Locator stream result..."); }

            return result.getOutputStream();
        }
    }

    protected XMLNode getXMLNodeFromObject(CallContext context, Object object) throws InvalidLocator {

        XMLNode xn = null;
        
        try {
            if (object instanceof File) {
                xn = XMLNode.createXMLNode(context, (File) object);
            } else if (object instanceof InputStream) {
                xn = XMLNode.createXMLNode(context, (InputStream) object, "");
            } else if (object instanceof XMLNode) {
                xn = (XMLNode) object;
            } else if (object instanceof TreeLeaf) {
                Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) object).getContent(context))).getData(context)));
                xn = XMLNode.createXMLNode(context, data instanceof Data_MediaObject_File ? new XMLInputSource(null, ((Data_MediaObject_File)(data)).getCurrentFile(context).getPath(), null) : new XMLInputSource(null, null, null, data.getStream(context), null), data.getDispositionFilename(context));
            } else if (object instanceof Data_MediaObject) {
                Data_MediaObject data = (Data_MediaObject) object;
                xn = XMLNode.createXMLNode(context, data instanceof Data_MediaObject_File ? new XMLInputSource(null, ((Data_MediaObject_File)(data)).getCurrentFile(context).getPath(), null) : new XMLInputSource(null, null, null, data.getStream(context), null), data.getDispositionFilename(context));
            } else {
                assert(false);
            }
        } catch (InvalidXML ix) {
            InvalidLocator.createAndThrow(context, ix, "Base '%(base)' for locator '%(locator)' contains invalid XML", "base", object, "locator", this.getTextLocatorValue(context));
            throw (InvalidLocator) null; // compiler insists
        }

        return xn;
    }

    protected Source createSource(CallContext context, File file, String resource_id) throws InvalidLocator {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            CustomaryContext.create(Context.create(context)).throwPreConditionViolation(context, fnfe, "File '%(file)' does not exist (while creating DOM source for transformation)", "file", file.getPath());
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }
        if (resource_id == null) {
            resource_id = "//File/" + file.getPath();
        }
        DOMSource source = null;
        try {
            source = new DOMSource(XMLUtil.parse(context, new XMLInputSource(null, null, null, new BufferedInputStream(fis), null), notification_level, resource_id));
        } catch (InvalidXML ix) {
            InvalidLocator.createAndThrow(context, ix, "Base '%(base)' for locator '%(locator)' contains invalid XML", "base", resource_id, "locator", this.getTextLocatorValue(context));
            throw (InvalidLocator) null; // compiler insists
        }
        source.setSystemId(resource_id);
        return source;
    }

    protected Source createSource(CallContext context, InputStream input_stream, String resource_id) throws InvalidLocator {
        DOMSource source = null;
        try {
            source = new DOMSource(XMLUtil.parse(context, new XMLInputSource(null, null, null, input_stream, null), notification_level, resource_id));
        } catch (InvalidXML ix) {
            InvalidLocator.createAndThrow(context, ix, "Base '%(base)' for locator '%(locator)' contains invalid XML", "base", resource_id, "locator", this.getTextLocatorValue(context));
            throw (InvalidLocator) null; // compiler insists
        }
        source.setSystemId(resource_id);
        return source;
    }

    protected SourceWithTimestamp getDOMSourceFromObject(CallContext context, Object object) throws InvalidLocator {

        SourceWithTimestamp swt = new SourceWithTimestamp(context, null, 0);

        if (object instanceof File) {
            swt.setSource(context, createSource(context, (File) object, null));
            swt.setLastModification(context, ((File) object).lastModified());
        } else if (object instanceof InputStream) {
            swt.setSource(context, createSource(context, (InputStream) object, null));
            swt.setLastModification(context, 0);
        } else if (object instanceof TreeLeaf) {
            Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) object).getContent(context))).getData(context)));
            String id = ((TreeLeaf) object).getLocation(context).getLocators(context).get(0).getTextLocator(context);
            if (data instanceof Data_MediaObject_File) {
                swt.setSource(context, createSource(context, (File) ((Data_MediaObject_File)(data)).getCurrentFile(context), id));
            } else {
                swt.setSource(context, createSource(context, data.getStream(context), id));
            }
            swt.setLastModification(context, data.getLastUpdate(context).getTime());
        } else if (object instanceof JavaResource) {
            TreeNode tn;
            try {
                tn = Factory_TreeNode_JavaResource.construct(context, ((JavaResource) object));
            } catch (ValidationFailure vf) {
                InvalidLocator.createAndThrow(context, vf, "Creation of java resource based XSLT transformation source failed in locator '%(locator)'", "locator", this.text_locator_value);
                throw (InvalidLocator) null;
            }
            Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) tn).getContent(context))).getData(context)));
            String id = "//JavaResource/" + ((JavaResource) object).getName(context);
            if (data instanceof Data_MediaObject_File) {
                swt.setSource(context, createSource(context, (File) ((Data_MediaObject_File)(data)).getCurrentFile(context), id));
            } else {
                swt.setSource(context, createSource(context, data.getStream(context), id));
            }
            swt.setLastModification(context, data.getLastUpdate(context).getTime());
        } else if (object instanceof Data_MediaObject_File) {
            File file = ((Data_MediaObject_File)object).getCurrentFile(context);
            swt.setSource(context, createSource(context, file, null));
            swt.setLastModification(context, ((Data_MediaObject_File)object).getLastUpdate(context).getTime());
        } else if (object instanceof Data_MediaObject) {
            swt.setSource(context, createSource(context, ((Data_MediaObject) object).getStream(context), null));
            swt.setLastModification(context, ((Data_MediaObject)object).getLastUpdate(context).getTime());
        } else {
            InvalidLocator.createAndThrow(context, "Cannot handle XSLT transformer source '%(source)'", "source", object);
            throw (InvalidLocator) null;
        }

        return swt;
    }

    protected SourceWithTimestamp getStreamSourceFromObject(CallContext context, Object object) throws InvalidLocator {

        SourceWithTimestamp swt = new SourceWithTimestamp(context, null, 0);

        if (object instanceof File) {
            swt.setSource(context, new StreamSource((File) object));
            swt.setLastModification(context, ((File) object).lastModified());
        } else if (object instanceof InputStream) {
            swt.setSource(context, new StreamSource((InputStream) object));
            swt.setLastModification(context, 0);
        } else if (object instanceof TreeLeaf) {
            Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) object).getContent(context))).getData(context)));
            if (data instanceof Data_MediaObject_File) {
                swt.setSource(context, new StreamSource((File) ((Data_MediaObject_File)(data)).getCurrentFile(context)));
            } else {
                swt.setSource(context, new StreamSource(data.getStream(context)));
                swt.getSource(context).setSystemId(((TreeLeaf) object).getLocation(context).getLocators(context).get(0).getTextLocator(context));
            }
            swt.setLastModification(context, data.getLastUpdate(context).getTime());
        } else if (object instanceof JavaResource) {
            TreeNode tn;
            try {
                tn = Factory_TreeNode_JavaResource.construct(context, ((JavaResource) object));
            } catch (ValidationFailure vf) {
                InvalidLocator.createAndThrow(context, vf, "Creation of java resource based XSLT transformation source failed in locator '%(locator)'", "locator", this.text_locator_value);
                throw (InvalidLocator) null;
            }
            Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) tn).getContent(context))).getData(context)));
            if (data instanceof Data_MediaObject_File) {
                swt.setSource(context, new StreamSource((File) ((Data_MediaObject_File)(data)).getCurrentFile(context)));
            } else {
                swt.setSource(context, new StreamSource(data.getStream(context)));
                swt.getSource(context).setSystemId(((JavaResource) object).getName(context));
            }
            swt.setLastModification(context, data.getLastUpdate(context).getTime());
        } else if (object instanceof Data_MediaObject_File) {
            swt.setSource(context, new StreamSource((File) ((Data_MediaObject_File)object).getCurrentFile(context)));
            swt.setLastModification(context, ((Data_MediaObject_File)object).getLastUpdate(context).getTime());
        } else if (object instanceof Data_MediaObject) {
            swt.setSource(context, new StreamSource(((Data_MediaObject) object).getStream(context)));
            swt.setLastModification(context, ((Data_MediaObject)object).getLastUpdate(context).getTime());
        } else {
            InvalidLocator.createAndThrow(context, "Cannot handle XSLT transformer source '%(source)'", "source", object);
            throw (InvalidLocator) null;
        }

        return swt;
    }
}
