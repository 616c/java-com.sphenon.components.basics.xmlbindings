package com.sphenon.basics.xmlbindings;

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
import com.sphenon.ui.frontends.jsp.*;
import com.sphenon.ui.frontends.alf.*;
import com.sphenon.basics.application.*;

public class XMLBindingsUtil {

    static public String getImageSubFolder (CallContext context) {
        WUIContext wc = WUIContext.get((Context) context);
        ALFContext ac = ALFContext.get((Context) context);

        String isf;

        if ((isf = (wc != null && wc.getWUISession(context) != null ? wc.getWUISession(context).getDisplayTargetProperty(context, "ImageSubFolder", (String) null) : null)) != null) { return isf; }

        if ((isf = (ac != null && ac.getConfiguration(context) != null ? ac.getConfiguration(context).getProperty(context, "ImageSubFolder", (String) null) : null)) != null) { return isf; }

        return ApplicationContext.getOrCreate((Context) context).getApplicationId(context).replace(".","/");
    }

    static public String getCurrentPageURL (CallContext context) {
        WUIContext wc = WUIContext.get((Context) context);
        ALFContext ac = ALFContext.get((Context) context);

        String cpu;

        if ((cpu = (wc != null && wc.getWUIParameters(context) != null
                    ? wc.getWUIParameters(context).createURL(context)
                    : null)
            ) != null) { return cpu; }

        if ((cpu = (ac != null
                    ? ac.getEmbeddedLinkPrefix(context)
                    : null)
            ) != null) { return cpu; }

        return "---unknown-page-url---";
    }
}
