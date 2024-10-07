package com.sphenon.basics.encoding;

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
import com.sphenon.basics.cache.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.expression.classes.*;
import com.sphenon.basics.services.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.data.conversion.*;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class EncodingService_DOCPAGE_HTML implements EncodingService {

    public EncodingService_DOCPAGE_HTML(CallContext context) {
    }

    public void notifyNewConsumer(CallContext context, Consumer consumer) {
        // nice to see you
    }

    public boolean equals(Object object) {
        return (object instanceof EncodingService_DOCPAGE_HTML);
    }

    public boolean canRecode(CallContext context, Encoding source, Encoding target) {
        return (source == Encoding.DOCPAGE && target == Encoding.HTML);
    }

    protected DataConverter converter;

    static protected Pattern space_pattern;

    public void recode(CallContext context, CharSequence source, Appendable target, Object... options) {
        if (source == null || source.length() == 0) { return; }
        
        if (space_pattern == null) {
            try {
                space_pattern = Pattern.compile("[^ \t\n\r]");
            } catch (Throwable t) {
            }
        }
        Matcher m = space_pattern.matcher(source);
        if (m.find() == false) { return; }

        if (converter == null) {
            converter = DataConversionManager.getConverter(context, "page2html");
        }

        /* this is internally HORRIBLE inperformant, mainly to
           stupid limiting incosistencies in Java API between
           streams, appenables, strings, stringbuffers, stringbuilders
           and alike; this affects parts of code over and over
        */

        Map arguments = options != null && options.length >= 2 ? ((Map) options[1]) : null;
        Data data_source = Data_MediaObject_CharSequence.create(context, source, "page");
        Data data_target = converter.convert(context, data_source, arguments);

        DataUtilities.copy(context, (Data_MediaObject) data_target, target);
    }
}
