package com.sphenon.basics.data.conversion;

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
import com.sphenon.basics.aspects.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.encoding.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.locating.*;
import com.sphenon.basics.locating.locators.*;
import com.sphenon.basics.locating.returncodes.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.metadata.*;
import com.sphenon.basics.security.*;
import com.sphenon.basics.variatives.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.locating.factories.*;
import com.sphenon.basics.validation.returncodes.*;

import java.io.*;
import java.util.regex.*;

public class Data_MediaObject_ConversionAdapter_XSLT implements Data_MediaObject {
    static protected long notification_level;
    static public    long adjustNotificationLevel(long new_level) { long old_level = notification_level; notification_level = new_level; return old_level; }
    static public    long getNotificationLevel() { return notification_level; }
    static { notification_level = NotificationLocationContext.getLevel(RootContext.getInitialisationContext(), "com.sphenon.basics.data.conversion.Data_MediaObject_ConversionAdapter_XSLT"); };

    protected Data_MediaObject     source_data;
    protected DynamicString        xslt_text_locator;
    protected Scope                dyns_scope;
    protected TypeImpl_MediaObject target_type;
    protected RegularExpression    filename_substitution;
    protected String               filename_substitution_regexp;
    protected String               filename_substitution_subst;
    protected String               disposition_filename;

    public Data_MediaObject_ConversionAdapter_XSLT (CallContext context, Data_MediaObject source_data, DynamicString xslt_text_locator, Scope dyns_scope, TypeImpl_MediaObject target_type, String filename_substitution_regexp, String filename_substitution_subst) {
        this.source_data                  = source_data;
        this.xslt_text_locator            = xslt_text_locator;
        this.dyns_scope                   = dyns_scope;
        this.target_type                  = target_type;
        this.filename_substitution_regexp = filename_substitution_regexp;
        this.filename_substitution_subst  = filename_substitution_subst;
    }

    public Type getDataType(CallContext context) {
        return target_type;
    }

    public String getMediaType(CallContext context) {
        return target_type.getMediaType(context);
    }

    public String getDispositionFilename(CallContext call_context) {
        Context context = Context.create(call_context);
        CustomaryContext cc = CustomaryContext.create(context);

        if (this.disposition_filename == null) {
            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Deriving filename: source name '%(sourcename)', regexp '%(regexp)', subexp '%(subexp)'", "sourcename", source_data.getDispositionFilename(context), "regexp", filename_substitution_regexp, "subexp", filename_substitution_subst); }

            this.filename_substitution = new RegularExpression(context, filename_substitution_regexp, filename_substitution_subst);
            this.disposition_filename = filename_substitution.replaceFirst(context, source_data.getDispositionFilename(context));

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "Deriving filename: result '%(result)'", "result", this.disposition_filename); }
        }
        return this.disposition_filename;
    }

    public java.util.Date getLastUpdate(CallContext call_context) {
        return source_data.getLastUpdate(call_context);
    }

    protected Authority authority = null;
    protected Authority getAuthority(CallContext context) {
        if (this.authority == null) {
            this.authority = SecuritySessionData.get(context).getAuthority(context);
        }
        return this.authority;
    }

    protected class StreamConverter extends PipedInputStream {
        protected Context context;
        protected CustomaryContext cc;

        protected LocatorXSLT       xslt_locator;

        protected java.lang.Process process;
        protected Thread            copy_thread;
        protected Thread            xslt_thread;

        protected InputStream       source_is;
        protected PipedOutputStream xslt_thread_output_pipe_os;
        protected PipedInputStream  xslt_thread_output_pipe_is;
        protected String            err;

        protected PipedOutputStream out;

        protected File              cache_file;
        protected FileOutputStream  cache_out;

        protected boolean           xslt_error;

        public StreamConverter (CallContext call_context, File cache_file_par, LocatorXSLT xslt_locator) {
            context = Context.create(call_context);
            cc = CustomaryContext.create(context);

            this.xslt_locator = xslt_locator;

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "creating StreamConverter..."); }

            this.cache_file = cache_file_par;
            if (this.cache_file != null) {
                if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, converting and writing to cache..."); }
                if (! this.cache_file.getParentFile().exists() && ! this.cache_file.getParentFile().mkdirs()) {
                    if ((notification_level & Notifier.MONITORING) != 0) { cc.sendSevereWarning(context, "StreamConverter, cache is not working (could not create parent directories of cache file), proceeding, but without caching; this may decrease performance significiantly"); }                    
                    this.cache_out = null;
                } else {
                    if (this.cache_file.exists() && ! this.cache_file.delete()) {
                        if ((notification_level & Notifier.MONITORING) != 0) { cc.sendSevereWarning(context, "StreamConverter, cache is not working (could not delete previous version of cache file), proceeding, but without caching; this may decrease performance significiantly"); }                    
                        this.cache_out = null;
                    } else {
                        try {
                            this.cache_out = new FileOutputStream(this.cache_file);
                        } catch (java.io.FileNotFoundException fnfe) {
                            if ((notification_level & Notifier.MONITORING) != 0) { cc.sendSevereWarning(context, "StreamConverter, cache is not working (could not open cache file [reason: '%(reason)']), proceeding, but without caching; this may decrease performance significiantly", "reason", fnfe); }
                            this.cache_out = null;
                        }
                    }
                }
            } else {
                this.cache_out = null;
            }

            try {
                out = new PipedOutputStream(this);
                xslt_thread_output_pipe_is = new PipedInputStream();
                xslt_thread_output_pipe_os = new PipedOutputStream(xslt_thread_output_pipe_is);
            } catch (java.io.IOException ioe) {
                cc.throwAssertionProvedFalse(context, ioe, "Piped Output Streams could not be created");
                throw (ExceptionAssertionProvedFalse) null; // compiler insists
            }
            
            xslt_locator.setOutputStream(context, xslt_thread_output_pipe_os);
            this.source_is = source_data.getStream(context);
            xslt_locator.setBaseObject(context, this.source_is);
            xslt_error = false;

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, performing xslt transformation '%(locator)'...", "locator", xslt_locator.getTextLocator(context)); }

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, creating copy thread..."); }

            this.copy_thread = new Thread() {
                    public void run () {
                        try {
                            int c;
                            while ((c = xslt_thread_output_pipe_is.read()) != -1) {
                                out.write(c);
                                if (cache_out != null) {
                                    cache_out.write(c);
                                }
                            }
                        } catch (java.io.IOException ioe) {
                            cc.sendError(context, "data converter copy thread terminated unsuccessfully: %(reason)", "reason", ioe);
                        } finally {
                            try {
                                out.close();
                                if (cache_out != null) {
                                    cache_out.close();
                                }
                            } catch (java.io.IOException ioe) {
                                cc.sendError(context, "data converter copy thread terminated (rather, somehow, not completely) unsuccessfully (during cleanup closing of out file): %(reason)", "reason", ioe);
                            }
                            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, copy thread terminated."); }
                        }
                    };
                };

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, starting copy thread..."); }

            copy_thread.start();

            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, creating xslt thread..."); }

            this.xslt_thread = new Thread() {
                    public void run () {
                        try {
                            StreamConverter.this.xslt_locator.retrieveTarget(context);
                        // } catch (InvalidLocator il) {
                        } catch (Throwable t) {
                            xslt_error = true;
                            cc.sendError(context, "StreamConverter, xslt error %(exception)", "exception", t);
                        }
                        try {
                            source_is.close();
                            xslt_thread_output_pipe_os.close();
                        } catch (java.io.IOException ioe) {
                            cc.sendError(context, "data converter xslt thread terminated (rather, somehow, not completely) unsuccessfully (during cleanup closing of in file): %(reason)", "reason", ioe);
                        }
                        if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, xslt thread terminated."); }
                        if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, waiting for copy thread to die..."); }
                        try {
                            copy_thread.join();
                        } catch (InterruptedException ie) {
                            cc.sendError(context, "data converter xslt thread was interrupted: %(reason)", "reason", ie);
                        }
                        if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, copy thread died, cleaning up..."); }
                        if (xslt_error && cache_file != null && cache_file.exists() && ! cache_file.delete()) {
                            if ((notification_level & Notifier.MONITORING) != 0) { cc.sendSevereWarning(context, "StreamConverter, cache is not working (could not delete cache file after conversion error occured), proceeding, but without caching; this may decrease performance significiantly"); }
                        }
                    };
                };
            
            if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, starting xslt thread..."); }

            this.xslt_thread.start();
        }
    }

    public java.io.InputStream getInputStream(CallContext call_context) {
        Context context = Context.create(call_context);
        CustomaryContext cc = CustomaryContext.create(context);

        String cachef = DataPackageInitialiser.getConfiguration(context).get(context, "conversion.CacheFolder", (String) null);
        boolean encode_cache_file_names = DataPackageInitialiser.getConfiguration(context).get(context, "conversion.EncodeCacheFileNames", false);
        File cache_file = null;
        if (cachef != null) {
            Locator origin = this.source_data.tryGetOrigin(context);
            if (origin != null) {
                String cachefilename = encode(context, origin.getTextLocator(context));
                if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, checking cache entry '%(entry)'...", "entry", cachefilename); }

                // type
                String tt = encode(context, this.target_type.getName(context));

                // file name
                String dfn = encode(context, this.getDispositionFilename(context));

                // lang
                String isolang = com.sphenon.basics.variatives.classes.StringPoolAccessor.getLanguage(context).split("[,+]")[0];
                if (isolang != null) { isolang = encode(context, isolang); }

                // security
                String security_identifier = getAuthority(context).getUnambiguousSecurityIdentifier(context);
                long lmosd = getAuthority(context).getLastModificationOfUserPermissions(context);

                // aspects
                AspectsContext ac = AspectsContext.get((Context) context);
                String aspects = (ac == null ? null : ac.getAspect(context).getName(context));
                if (aspects != null) { aspects = encode(context, aspects); }

                cachefilename += "/" + tt + "-" + dfn + (isolang == null ? "" : ("-HL-" + isolang)) + (security_identifier == null ? "" : ("-SI-" + security_identifier)) + (aspects == null ? "" : ("-AS-" + aspects));

                int file_idx = 1;
                String fullcachefilepath = cachef + "/" + (encode_cache_file_names ? Encoding.recode(context, cachefilename + (file_idx == 1 ? "" : ("-" + file_idx)), Encoding.UTF8, Encoding.SHA1) : cachefilename);
                if (encode_cache_file_names) {
                    do {
                        File cache_desc_file = new File(fullcachefilepath + ".desc");
                        if (cache_desc_file.exists()) {
                            String cur = FileUtilities.readFile(context, cache_desc_file).get(0);
                            if (cur.equals(cachefilename) == false) {
                                file_idx++;
                            } else {
                                break;
                            }
                        } else {
                            FileUtilities.writeFile(context, cache_desc_file, cachefilename);
                        }
                    } while(true);
                }
                cache_file = new File(fullcachefilepath);

                if (cache_file.exists() && cache_file.lastModified() > this.source_data.getLastUpdate(context).getTime() && lmosd != -1 && cache_file.lastModified() > lmosd) {
                    if ((notification_level & Notifier.SELF_DIAGNOSTICS) != 0) { cc.sendTrace(context, Notifier.SELF_DIAGNOSTICS, "StreamConverter, cache entry is valid, using it..."); }
                    try {
                        return new FileInputStream(cache_file);
                    } catch (java.io.FileNotFoundException fnfe) {
                        cc.throwImpossibleState(context, fnfe, "'exists' method reports 'true' for '%(filename)', but opening for read fails with 'file not found'", "filename", cache_file.getName());
                        throw (ExceptionImpossibleState) null; // compiler insists
                    }
                }
            }
        }                

        LocatorXSLT xslt_locator = null;
        String current_text_locator = this.xslt_text_locator.get(context, this.dyns_scope);
        try {
            xslt_locator = (LocatorXSLT) Factory_Locator.construct(context, current_text_locator);
        } catch (ValidationFailure vf) {
            CustomaryContext.create((Context)context).throwConfigurationError(context, vf, "Locator for XSLT data conversion is invalid: '%(textlocator)'", "textlocator", current_text_locator);
            throw (ExceptionConfigurationError) null; // compiler insists
        } catch (ClassCastException cce) {
            CustomaryContext.create((Context)context).throwConfigurationError(context, cce, "Locator for XSLT data conversion is not a XSLT Locator: '%(textlocator)'", "textlocator", current_text_locator);
            throw (ExceptionConfigurationError) null; // compiler insists
        }

        return new StreamConverter(context, cache_file, xslt_locator);
    }

    public java.io.InputStream getStream(CallContext context) {
        return this.getInputStream(context);
    }

    public java.io.OutputStream getOutputStream(CallContext context) {
        CustomaryContext.create((Context)context).throwLimitation(context, "Data_MediaObject_ConversionAdapter_XSLT is not writable");
        throw (ExceptionLimitation) null; // compilernsists
    }

    final static String[] hex =
    {
        "00", "01", "02", "03", "04", "05", "06", "07",
        "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
        "10", "11", "12", "13", "14", "15", "16", "17",
        "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
        "20", "21", "22", "23", "24", "25", "26", "27",
        "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
        "30", "31", "32", "33", "34", "35", "36", "37",
        "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
        "40", "41", "42", "43", "44", "45", "46", "47",
        "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
        "50", "51", "52", "53", "54", "55", "56", "57",
        "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
        "60", "61", "62", "63", "64", "65", "66", "67",
        "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
        "70", "71", "72", "73", "74", "75", "76", "77",
        "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
        "80", "81", "82", "83", "84", "85", "86", "87",
        "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
        "90", "91", "92", "93", "94", "95", "96", "97",
        "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
        "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7",
        "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
        "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7",
        "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
        "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7",
        "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
        "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7",
        "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
        "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7",
        "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
        "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
        "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"
    };

    static public String encode(CallContext call_context, String string) {
        byte[] bytes;
        try {
            bytes = string.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            Context context = Context.create(call_context);
            CustomaryContext cc = CustomaryContext.create(context);
            cc.throwLimitation(context, "System (VM) does not support UTF-8 encoding");
            throw (ExceptionLimitation) null;
        }
        StringBuffer new_string = new StringBuffer();
        int b;
        
        boolean was_slash = false;
        for (int i=0; i < bytes.length; i++) {
            b = bytes[i];
            if (b < 0) { b += 256; }

            if (    (b >= 'A' && b <= 'Z')
                 || (b >= 'a' && b <= 'z')
                 || (b >= '0' && b <= '9')
                 || (b == '/')
               )
            {
                if (was_slash && b == '/') {
                    new_string.append('_');
                }
                new_string.append((char)b);
            } else if (b == '\\') {
                new_string.append("__/");
            } else {
                new_string.append("_"+hex[b]);
            }
            was_slash = (b == '/');
        }
        return new_string.toString();
    }  

    public Locator tryGetOrigin(CallContext context) {
        return source_data.tryGetOrigin(context);
    }
}

