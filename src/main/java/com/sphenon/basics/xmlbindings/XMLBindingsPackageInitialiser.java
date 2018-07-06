package com.sphenon.basics.xmlbindings;

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
import com.sphenon.basics.notification.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.services.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.data.conversion.*;
import com.sphenon.basics.metadata.*;
import com.sphenon.basics.xml.*;

public class XMLBindingsPackageInitialiser {

    static protected boolean initialised = false;

    static {
        initialise(RootContext.getRootContext());
    }

    static public void initialise () {
        initialise (RootContext.getRootContext());
    }

    static public void initialise (CallContext context) {
        
        if (initialised == false) {
            initialised = true;

            XMLPackageInitialiser.initialise(context);

            Configuration.loadDefaultProperties(context, com.sphenon.basics.xmlbindings.XMLBindingsPackageInitialiser.class);

            loadDataConverter (context, getConfiguration(context));

            ServiceRegistry.registerService(context, new com.sphenon.basics.encoding.EncodingService_DOCPAGE_HTML(context));
        }
    }

    static protected Configuration config;
    static public Configuration getConfiguration (CallContext context) {
        if (config == null) {
            config = Configuration.create(RootContext.getInitialisationContext(), "com.sphenon.basics.xmlbindings");
        }
        return config;
    }

    static protected void loadDataConverter (CallContext context, Configuration configuration) {
        String textlocator;
        String property_prefix;
        int entry_number = 0;
        while ((textlocator = configuration.get(context, (property_prefix = "conversion.DataConverter." + ++entry_number) + ".Locator", (String) null)) != null) {
            DataConverter dc = processEntry(context, configuration, property_prefix, textlocator);

            boolean register_by_id = configuration.get(context, property_prefix + ".RegisterById", true);
            boolean register_by_type = configuration.get(context, property_prefix + ".RegisterByType", true);

            if (dc != null) {
                DataConversionManager.getSingleton(context).register(context, dc, register_by_id, register_by_type);
            }
        }
    }

    static public DataConverter processEntry(CallContext context, Configuration configuration, String property_prefix, String textlocator) {
        CustomaryContext cc = CustomaryContext.create((Context)context);

        String property;

        property = property_prefix + ".Id";
        String id = configuration.get(context, property, (String) null);

        property = property_prefix + ".SourceType";
        String source_type = configuration.get(context, property, (String) null);
        if (source_type == null) {
            cc.throwConfigurationError(context, "No property '%(property)' found", "property", property);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        property = property_prefix + ".TargetType";
        String target_type = configuration.get(context, property, (String) null);
        if (target_type == null) {
            cc.throwConfigurationError(context, "No property '%(property)' found", "property", property);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        property = property_prefix + ".Filename.RegExp";
        String filename_substitution_regexp = configuration.get(context, property, (String) null);
        if (filename_substitution_regexp == null) {
            cc.throwConfigurationError(context, "No property '%(property)' found", "property", property);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        property = property_prefix + ".Filename.Substitution";
        String filename_substitution_subst = configuration.get(context, property, (String) null);
        if (filename_substitution_subst == null) {
            cc.throwConfigurationError(context, "No property '%(property)' found", "property", property);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        return new DataConverter_MediaObject_XSLT (context,
                                                   id,
                                                   (TypeImpl_MediaObject) TypeManager.getMediaType(context, source_type),
                                                   (TypeImpl_MediaObject) TypeManager.getMediaType(context, target_type), 
                                                   textlocator, 
                                                   filename_substitution_regexp, 
                                                   filename_substitution_subst);
    }
}
