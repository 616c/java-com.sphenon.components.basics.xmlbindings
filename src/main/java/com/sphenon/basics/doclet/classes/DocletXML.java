package com.sphenon.basics.doclet.classes;

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
import com.sphenon.basics.tracking.*;
import com.sphenon.basics.tracking.classes.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.expression.*;
import com.sphenon.basics.encoding.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.quantities.*;
import com.sphenon.basics.monitoring.*;
import com.sphenon.basics.accessory.classes.*;
import com.sphenon.basics.xml.*;
import com.sphenon.engines.generator.*;
import com.sphenon.engines.generator.classes.*;

import com.sphenon.basics.doclet.*;

import java.util.Vector;
import java.util.regex.*;

public class DocletXML extends Class_MonitorableCoreObject implements Doclet, OriginAware {

    protected XMLNode xml_node;
    protected Doclet parent;

    protected String default_maturity;
    protected String default_security_class;

    public DocletXML (CallContext context, XMLNode xml_node, Doclet parent) {
        this(context, xml_node, parent, null);
    }

    public DocletXML (CallContext context, XMLNode xml_node, Doclet parent, Origin origin) {
        this(context, xml_node, parent, origin, "Scratch", "Default");
    }

    public DocletXML (CallContext context, XMLNode xml_node, Doclet parent, Origin origin, String default_maturity, String default_security_class) {
        this.xml_node = xml_node;
        this.parent = parent;
        this.origin = origin;
        this.default_maturity = default_maturity;
        this.default_security_class = default_security_class;
    }

    public void validate(CallContext context) {
        this.getSecurityClass(context);
        this.getAudience(context);
        this.getIntent(context);
        this.getExtent(context);
        this.getCoverage(context);
        this.getMaturity(context);
        this.getForm(context);
        this.getLayout(context);
        this.getStyle(context);
        this.getEncoding(context);
        this.getAspect(context);
        this.getEntity (context);
        this.getEntityType (context);
        this.getEntityVersion (context);
        this.getDocletVersion (context);
        this.getLanguage (context);
    }

    public void transferProblemsTo(CallContext context, ProblemMonitor target) {
        this.validate(context);
        super.transferProblemsTo(context, target);
    }

    protected String docbook;
    static protected RegularExpression gid = new RegularExpression("^ *<\\?g +(G-[0-9\\.]+-[A-Za-z0-9_]+-[0-9\\.]+) *\\?> *\n");

    public String getDocBook (CallContext context) {
        if (this.docbook == null) {
            XMLNode trimmed = this.xml_node.getChilds(context).trim(context);
            String content = trimmed != null && trimmed.getDOMNodes(context).size() == 1 ? trimmed.serialise(context, false) : this.xml_node.toString(context);
            boolean need_wrapper = (trimmed == null || trimmed.getDOMNodes(context).size() > 1);

            Matcher matcher = gid.getMatcher(context, content);
            String[] matches = gid.tryGetMatches(context, matcher);
            if (    matches != null
                 && matches.length == 1
                ) {
                String template = matches[0] + '\n' + content.substring(matcher.end());
                Generator generator = GeneratorRegistry.get(context).getGenerator_InMemoryTemplate(context, template);
                GeneratorOutputToString gots = new GeneratorOutputToString(context);
                generator.generate(context, gots);
                content = gots.getResult(context);
            }

            if (need_wrapper) {
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><section xmlns:xlink=\"http://www.w3.org/1999/xlink\"><para>");
                sb.append(content);
                sb.append("</para></section>");
                this.docbook = sb.toString();
            } else {
                this.docbook = content;
            }
        }
        return this.docbook;
    }

    protected String doclet_type;
    static protected RegularExpression doclet_type_format = new RegularExpression("^[A-Za-z0-9_]+$");

    public String getDocletType(CallContext context) {
        if (this.doclet_type == null) {
            this.doclet_type = this.xml_node.getName(context).replaceFirst("^docl:", "");
            this.check(context, doclet_type, doclet_type_format, "DocletType");
        }
        return this.doclet_type;
    }

    protected String security_class;
    static protected RegularExpression security_class_format = new RegularExpression("^[A-Za-z0-9_]+$");

    public String getSecurityClass(CallContext context) {
        if (this.security_class == null) {
            String local_security_class = this.xml_node.getAttribute(context, "SecurityClass");
            if (local_security_class == null || local_security_class.length() == 0) {
                local_security_class = this.default_security_class;
            }
            this.check(context, local_security_class, security_class_format, "SecurityClass");
            String parent_security_class = this.parent == null ? null : this.parent.getSecurityClass(context);
            this.security_class = parent_security_class == null ? local_security_class : createListUnion(context, parent_security_class, local_security_class);
        }
        return this.security_class;
    }

    protected String audience;
    static protected RegularExpression audience_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:\\|[A-Za-z0-9_]+)*)?$");

    public String getAudience(CallContext context) {
        if (this.audience == null) {
            String local_audience = this.xml_node.getAttribute(context, "Audience");
            this.check(context, local_audience, audience_format, "Audience");
            String parent_audience = this.parent == null ? null : this.parent.getAudience(context);
            this.audience = parent_audience == null ? local_audience : createListIntersection(context, parent_audience, local_audience, "Audience");
        }
        return this.audience;
    }

    protected String intent;
    static protected RegularExpression intent_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:\\|[A-Za-z0-9_]+)*)?$");

    public String getIntent(CallContext context) {
        if (this.intent == null) {
            String local_intent = this.xml_node.getAttribute(context, "Intent");
            this.check(context, local_intent, intent_format, "Intent");
            String parent_intent = this.parent == null ? null : this.parent.getIntent(context);
            this.intent = parent_intent == null ? local_intent : createListIntersection(context, parent_intent, local_intent, "Intent");
        }
        return this.intent;
    }

    protected String extent;
    static protected RegularExpression extent_format = new RegularExpression("^[A-Za-z0-9_]*$");

    public String getExtent(CallContext context) {
        if (this.extent == null) {
            this.extent = this.xml_node.getAttribute(context, "Extent");
            this.check(context, extent, extent_format, "Extent");
        }
        return this.extent;
    }

    protected String coverage;
    static protected RegularExpression coverage_format = new RegularExpression("^[A-Za-z0-9_]*$");

    public String getCoverage(CallContext context) {
        if (this.coverage == null) {
            this.coverage = this.xml_node.getAttribute(context, "Coverage");
            this.check(context, coverage, coverage_format, "Coverage");
        }
        return this.coverage;
    }

    protected String maturity;
    static protected RegularExpression maturity_format = new RegularExpression("^Scratch|Draft|Final$");

    public String getMaturity(CallContext context) {
        if (this.maturity == null) {
            String local_maturity = this.xml_node.getAttribute(context, "Maturity");
            boolean is_group = (this.getDocletType(context) != null && this.getDocletType(context).matches("Doclet|Group"));
            if (local_maturity == null || local_maturity.length() == 0) {
                local_maturity = is_group ? "Final" : this.default_maturity;
            }
            this.check(context, local_maturity, maturity_format, "Maturity");
            String parent_maturity = this.parent == null ? null : this.parent.getMaturity(context);
            if (parent_maturity != null) {
                if (parent_maturity.equals("Scratch")) {
                    local_maturity = "Scratch";
                } else if (parent_maturity.equals("Draft") && local_maturity.equals("Final")) {
                    local_maturity = "Draft";
                }
            }
            this.maturity = local_maturity;
        }
        return this.maturity;
    }

    protected String form;
    static protected RegularExpression form_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:\\|[A-Za-z0-9_]+)*)?$");

    public String getForm(CallContext context) {
        if (this.form == null) {
            String local_form = this.xml_node.getAttribute(context, "Form");
            this.check(context, local_form, form_format, "Form");
            String parent_form = this.parent == null ? null : this.parent.getForm(context);
            this.form = parent_form == null ? local_form : createListIntersection(context, parent_form, local_form, "Form");
        }
        return this.form;
    }

    protected String layout;
    static protected RegularExpression layout_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:\\|[A-Za-z0-9_]+)*)?$");

    public String getLayout(CallContext context) {
        if (this.layout == null) {
            String local_layout = this.xml_node.getAttribute(context, "Layout");
            this.check(context, local_layout, layout_format, "Layout");
            String parent_layout = this.parent == null ? null : this.parent.getLayout(context);
            this.layout = parent_layout == null ? local_layout : createListIntersection(context, parent_layout, local_layout, "Layout");
        }
        return this.layout;
    }

    protected String[] style;
    static protected RegularExpression style_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:[,:/ ]+[A-Za-z0-9_]+)*)?$");

    public String[] getStyle(CallContext context) {
        if (this.style == null) {
            String style_string = this.xml_node.getAttribute(context, "Style");
            if (style_string == null || style_string.length() == 0) {
                return null;
            }
            this.check(context, style_string, style_format, "Style");
            this.style = style_string.split("[,:/ ]+");
        }
        return this.style;
    }

    protected Encoding encoding;

    public Encoding getEncoding(CallContext context) {
        if (this.encoding == null) {
            String encoding_string = this.xml_node.getAttribute(context, "Encoding");
            if (encoding_string == null || encoding_string.length() == 0) {
                return null;
            }
            this.encoding = Encoding.getEncoding(context, encoding_string);
            if (this.encoding == null) {
                CustomaryContext.create((Context)context).throwConfigurationError(context, "Doclet contains undefined encoding '%(encoding)'", "encoding", encoding);
                throw (ExceptionConfigurationError) null; // compiler insists
            }
        }
        return this.encoding;
    }

    protected String aspect;
    static protected RegularExpression aspect_format = new RegularExpression("^(?:[A-Za-z0-9_]+(?:/[A-Za-z0-9_]+)*)?$");

    public String getAspect(CallContext context) {
        if (this.aspect == null) {
            String local_aspect = this.xml_node.getAttribute(context, "Aspect");
            this.check(context, local_aspect, aspect_format, "Aspect");
            String parent_aspect = this.parent == null ? null : this.parent.getAspect(context);
            this.aspect = parent_aspect == null || parent_aspect.length() == 0 ? local_aspect : local_aspect == null || local_aspect.length() == 0 ? parent_aspect : parent_aspect + "/" + local_aspect;
        }
        return this.aspect;
    }

    protected String entity;
    static protected RegularExpression ere = new RegularExpression("^(?:oorl:)?(?:(//[^/:]+:)([^/]+))?(.*)");

    public String getEntity (CallContext context) {
        if (this.entity == null) {
            String local_entity = this.xml_node.getAttribute(context, "Entity");
            String parent_entity = this.parent == null ? null : this.parent.getEntity(context);
            String entity_type = this.getEntityType(context);

            if (parent_entity != null && entity_type != null) {
                String[] pe = ere.tryGetMatches(context, parent_entity);
                if (pe[1] != null && entity_type != null && entity_type.isEmpty() == false) {
                    parent_entity = (pe[0] == null ? "" : pe[0])
                                  + entity_type
                                  + (pe[2] == null ? "" : pe[2]);
                }
            }
            this.entity = parent_entity == null || parent_entity.length() == 0 ? local_entity : local_entity == null || local_entity.length() == 0 ? parent_entity : parent_entity + "/" + local_entity;
        }
        return this.entity;
    }

    protected String entity_type;
    static protected RegularExpression entity_type_format = new RegularExpression("^[A-Za-z0-9_]+$");

    public String getEntityType (CallContext context) {
        if (this.entity_type == null) {
            this.entity_type = this.xml_node.getAttribute(context, "EntityType");
            String entity = this.xml_node.getAttribute(context, "Entity");
            if (entity == null || entity.length() == 0) {
                if (this.entity_type != null && this.entity_type.length() != 0) {
                    this.addProblemStatus(context, ProblemState.WARNING, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet property 'EntityType', value '%(value)' must be empty if 'Entity' is empty", "value", this.entity_type));
                }
            } else {
                this.check(context, entity_type, entity_type_format, "EntityType");
            }
        }
        return this.entity_type;
    }

    protected String entity_version;
    static protected RegularExpression entity_version_format = new RegularExpression("^(?:" + VersionNumber.format + ")?$");

    public String getEntityVersion (CallContext context) {
        if (this.entity_version == null) {
            this.entity_version = this.xml_node.getAttribute(context, "EntityVersion");
            this.check(context, entity_version, entity_version_format, "EntityVersion");
        }
        return this.entity_version;
    }

    protected String doclet_version;
    static protected RegularExpression doclet_version_format = new RegularExpression("^(?:" + VersionNumber.format + ")?$");

    public String getDocletVersion (CallContext context) {
        if (this.doclet_version == null) {
            this.doclet_version = this.xml_node.getAttribute(context, "DocletVersion");
            this.check(context, doclet_version, doclet_version_format, "DocletVersion");
        }
        return this.doclet_version;
    }

    protected String language;
    static protected RegularExpression language_format = new RegularExpression("^(?:[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)?$");

    public String getLanguage (CallContext context) {
        if (this.language == null) {
            this.language = this.xml_node.getAttribute(context, "Language");
            this.check(context, language, language_format, "Language");
        }
        return this.language;
    }

    protected void check(CallContext context, String value, RegularExpression format, String property) {
        if (value == null) { value = ""; }
        if (format.matches(context, value) == false) {
            this.addProblemStatus(context, ProblemState.WARNING, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet property '%(property)' has invalid syntax '%(local)', should match '%(format)'", "property", property, "local", value, "format", format));
        }
    }

    static protected String[] empty = new String[0];

    protected String createListIntersection(CallContext context, String parent_list, String local_list, String property) {
        if (parent_list == null || parent_list.length() == 0) {
            return local_list;
        }
        if (local_list == null || local_list.length() == 0) {
            return parent_list;
        }
        String[] parent_parts = parent_list.split("\\|");
        String[] local_parts = local_list.split("\\|");
        Vector<String> intersection = new Vector<String>();
        for (String local_part : local_parts) {
            boolean included = false;
            for (String parent_part : parent_parts) {
                if (parent_part.equals(local_part)) {
                    intersection.add(local_part);
                    included = true;
                    break;
                }
            }
            if (included == false) {
                this.addProblemStatus(context, ProblemState.CAUTION, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet property '%(property)', contains more local options '%(local)' than parent options '%(parent)', some of them can never be matched", "property", property, "local", local_list, "parent", parent_list));
            }
        }
        return intersection.size() == 0 ? "" : StringUtilities.join(context, intersection.toArray(empty), "|", true);
    }

    protected String createListUnion(CallContext context, String parent_list, String local_list) {
        if (parent_list == null || parent_list.length() == 0) {
            return local_list;
        }
        if (local_list == null || local_list.length() == 0) {
            return parent_list;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(parent_list);
        sb.append("|");
        sb.append(local_list);
        return sb.toString();
    }

    /********************************************************************************************/
    /* IMPORTANT! These methods are duplicated from Class_OriginAware
       (no multiple inheritance in java)                                                        */
    /********************************************************************************************/

    protected Origin origin;

    public Origin getOrigin (CallContext context) {
        return this.origin;
    }

    public void setOrigin (CallContext context, Origin origin) {
        this.origin = origin;
    }
}
