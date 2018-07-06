package com.sphenon.basics.xml.factories;

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
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.factories.*;
import com.sphenon.basics.locating.returncodes.*;
import com.sphenon.basics.graph.*;
import com.sphenon.basics.graph.javaresources.factories.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.validation.returncodes.*;

import com.sphenon.basics.xml.*;
import com.sphenon.basics.xml.returncodes.*;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Factory_XMLNode {

    public Factory_XMLNode (CallContext context) {
    }

    static public XMLNode construct(CallContext context, String locator) {
        return construct(context, Factory_Locator.tryConstruct(context, locator));
    }

    static public XMLNode construct(CallContext context, Location location) {
        Object object = null;
        try {
            object = location.retrieveTarget(context, null);
        } catch (InvalidLocator il) {
            CustomaryContext.create((Context)context).throwPreConditionViolation(context, il, "Invalid location '%(location)' passed to XMLNode factory", "location", location);
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }
        return constructByObject(context, object, location);
    }

    static public XMLNode construct(CallContext context, Locator locator) {
        Object object = null;
        try {
            object = locator.retrieveTarget(context, null);
        } catch (InvalidLocator il) {
            CustomaryContext.create((Context)context).throwPreConditionViolation(context, il, "Invalid locator '%(locator)' passed to XMLNode factory", "locator", locator.getPartialTextLocator(context));
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }
        return constructByObject(context, object, locator);
    }

    static public XMLNode constructByObject(CallContext context, Object object) {
        return constructByObject(context, object, "");
    }

    static protected XMLNode constructByObject(CallContext context, Object object, Object info) {
        InputStream input_stream = null;

        try {
            if (object instanceof XMLNode) {
                return (XMLNode) object;
            } else if (object instanceof String) {
                return construct(context, (String) object);
            } else if (object instanceof Locator) {
                return construct(context, (Locator) object);
            } else if (object instanceof Location) {
                return construct(context, (Location) object);
            } else if (object instanceof File) {
                input_stream = new FileInputStream((File) object);
            } else if (object instanceof InputStream) {
                input_stream = (InputStream) object;
            } else if (object instanceof TreeLeaf) {
                Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) object).getContent(context))).getData(context)));
                if (data instanceof Data_MediaObject_File) {
                    input_stream = new FileInputStream((File) ((Data_MediaObject_File)(data)).getCurrentFile(context));
                } else {
                    input_stream = data.getStream(context);
                }
            } else if (object instanceof JavaResource) {
                TreeNode tn;
                try {
                    tn = Factory_TreeNode_JavaResource.construct(context, ((JavaResource) object));
                } catch (ValidationFailure vf) {
                    CustomaryContext.create((Context)context).throwPreConditionViolation(context, vf, "Creation of java resource based locator '%(locator)' failed", "locator", info);
                    throw (ExceptionPreConditionViolation) null; // compiler insists
                }
                Data_MediaObject data = ((Data_MediaObject)(((NodeContent_Data)(((TreeLeaf) tn).getContent(context))).getData(context)));
                if (data instanceof Data_MediaObject_File) {
                    input_stream = new FileInputStream((File) ((Data_MediaObject_File)(data)).getCurrentFile(context));
                } else {
                    input_stream = data.getStream(context);
                }
            } else if (object instanceof Data_MediaObject_File) {
                input_stream = new FileInputStream((File) ((Data_MediaObject_File)(object)).getCurrentFile(context));
            } else if (object instanceof Data_MediaObject) {
                input_stream = ((Data_MediaObject) object).getStream(context);
            } else {
                CustomaryContext.create((Context)context).throwPreConditionViolation(context, "Cannot create a XMLNode from '%(locator)'", "locator", info);
                throw (ExceptionPreConditionViolation) null; // compiler insists
            }
        } catch (FileNotFoundException fnfe) {
            CustomaryContext.create((Context)context).throwPreConditionViolation(context, fnfe, "File denoted by locator '%(locator)' does not exist", "locator", info);
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }
            
        try {
            return XMLNode.createXMLNode(context, input_stream, info instanceof ContextAware ? ((ContextAware) info).toString(context) : info.toString());
        } catch (InvalidXML ix) {
            CustomaryContext.create((Context)context).throwPreConditionViolation(context, ix, "Could not parse xml denoted by locator '%(locator)'", "locator", info);
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }
    }
}
