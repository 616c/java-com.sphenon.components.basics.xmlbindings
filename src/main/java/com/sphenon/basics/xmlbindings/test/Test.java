package com.sphenon.basics.xmlbindings.test;

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
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.locating.*;

import com.sphenon.basics.xml.*;
import com.sphenon.basics.xmlbindings.*;

import java.io.File;

public class Test {

    public static void main(String[] args) {

        Context context = com.sphenon.basics.context.classes.RootContext.getRootContext();

        try {
            {
                String tn = "ctn://XPath//A/B";
                Locator locator = Locator.createLocator(context, tn);
                locator.setBaseObject(context, new File("test.xml"));
                XMLNode xnr = (XMLNode) locator.retrieveTarget(context);
                System.err.println(tn + " => " + (xnr.exists(context) ? xnr : "not found") + " (should be found)");
            }
            
            {
                String tn = "ctn://File/test.xml/,//XPath//A/B";
                Locator locator = Locator.createLocator(context, tn);
                XMLNode xnr = (XMLNode) locator.retrieveTarget(context);
                System.err.println(tn + " => " + (xnr.exists(context) ? xnr : "not found") + " (should be found)");
            }
            
            {
                String tn = "ctn://XPath//A/B";
                Locator locator = Locator.createLocator(context, tn);
                LocatingContext lc = LocatingContext.create(context);
                lc.registerObject(context, "file", new File("test.xml"));
                XMLNode xnr = (XMLNode) locator.retrieveTarget(context);
                System.err.println("(base via lc) " + tn + " => " + (xnr.exists(context) ? xnr : "not found") + " (should be found)");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
