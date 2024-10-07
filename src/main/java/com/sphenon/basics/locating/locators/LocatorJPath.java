package com.sphenon.basics.locating.locators;

/****************************************************************************
  Copyright 2001-2024 Sphenon GmbH

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

import com.sphenon.formats.json.*;
import com.sphenon.formats.json.factories.*;
import com.sphenon.formats.json.returncodes.*;

import com.sphenon.basics.xmlbindings.*;

import java.io.File;
import java.io.InputStream;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

public class LocatorJPath extends Locator {
    static protected Configuration config;
    static { config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.locating.locators.LocatorJPath"); };
    static {
        XMLBindingsPackageInitialiser.initialise();
    }

    public LocatorJPath (CallContext context, String text_locator_value, Locator sub_locator, String locator_class_parameter_string) {
        super(context, text_locator_value, sub_locator, locator_class_parameter_string);
    }

    /* Parser States -------------------------------------------------------------------- */

    static protected LocatorParserState[] locator_parser_state;
        
    protected LocatorParserState[] getParserStates(CallContext context) {
        if (locator_parser_state == null) {
            locator_parser_state = new LocatorParserState[] {
                new LocatorParserState(context, "property", "property::String:0", false, true, Object.class)
            };
        }
        return locator_parser_state;
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
            locator_base_acceptors.add(new LocatorBaseAcceptor(context, JSONNode.class));
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
                new LocatorClassParameter(context, "targettype", "JSONNode|Text", "JSONNode")
            };
        }
        return locator_class_parameters;
    }
    
    /* ---------------------------------------------------------------------------------- */

    public String getTargetVariableName(CallContext context) {
        return "json_node";
    }

    protected Object retrieveLocalTarget(CallContext context) throws InvalidLocator {
        Object base = lookupBaseObject(context, true);

        JSONNode xn = null;
        try {
            xn = Factory_JSONNode.constructByObject(context, base);
        } catch (Throwable t) {
            InvalidLocator.createAndThrow(context, t, "Base '%(base)' for locator '%(locator)' contains invalid JSON", "base", base, "locator", this.getTextLocatorValue(context));
            throw (InvalidLocator) null; // compiler insists
        }

        LocatorStep[] steps = getLocatorSteps(context);

        for (LocatorStep step : steps) {
            String sa = step.getAttribute(context);
            String sv = step.getValue(context);
            if (sv == null || sv.length() == 0) { continue; }
            if (sv.matches("^[0-9]+$")) {
                if (xn.isArray(context)) {
                    int index = Integer.parseInt(sv);
                    xn = xn.getChild(context, index);
                    if (xn == null) {
                        InvalidLocator.createAndThrow(context, "JSONNode of array in step '%(value)' in locator '%(locator)' does not exist", "value", sv, "locator", this.getTextLocatorValue(context));
                        throw (InvalidLocator) null; // compiler insists
                    }
                } else {
                    InvalidLocator.createAndThrow(context, "JSONNode in step '%(value)' in locator '%(locator)' cannot be applied since it's not an array", "value", sv, "locator", this.getTextLocatorValue(context));
                    throw (InvalidLocator) null; // compiler insists
                }
            } else {
                if (xn.isObject(context)) {
                    xn = xn.getChild(context, sv);
                    if (xn == null) {
                        InvalidLocator.createAndThrow(context, "JSONNode of object in step '%(value)' in locator '%(locator)' does not exist", "value", sv, "locator", this.getTextLocatorValue(context));
                        throw (InvalidLocator) null; // compiler insists
                    }
                } else {
                    InvalidLocator.createAndThrow(context, "JSONNode in step '%(value)' in locator '%(locator)' cannot be applied since it's not an object", "value", sv, "locator", this.getTextLocatorValue(context));
                    throw (InvalidLocator) null; // compiler insists
                }
            }
        }

        String ltype = getLocatorClassParameter(context, "targettype");

        if (ltype.equals("JSONNode")) {
            return xn;
        } else {
            return xn.toText(context);
        }
    }
}
