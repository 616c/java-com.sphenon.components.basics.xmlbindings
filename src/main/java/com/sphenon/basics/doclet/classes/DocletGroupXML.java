package com.sphenon.basics.doclet.classes;

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
import com.sphenon.basics.tracking.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.encoding.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.xml.*;
import com.sphenon.basics.accessory.*;
import com.sphenon.basics.monitoring.*;

import com.sphenon.basics.doclet.*;

import java.util.Vector;

public class DocletGroupXML extends Class_DocletGroupBase implements OriginAware {

    protected XMLNode             xml_node;
    protected Vector<DocletGroup> sub_entities;
    protected Vector<Doclet>      doclets;
    protected Doclet              local_meta_data;

    public DocletGroupXML (CallContext context, XMLNode xml_node) {
        this(context, xml_node, null, null);
    }

    public DocletGroupXML (CallContext context, XMLNode xml_node, Doclet parent_meta_data) {
        this(context, xml_node, parent_meta_data, null);
    }

    public DocletGroupXML (CallContext context, XMLNode xml_node, Doclet parent_meta_data, Origin origin) {
        super(context);
        if (xml_node.isDocument(context)) {
            xml_node = xml_node.getChildElementsByRegExp(context);
        }
        this.xml_node = xml_node;
        this.origin = origin;
        this.local_meta_data = this.processChilds(context, xml_node, parent_meta_data);
    }

    static public class Result {
        public Vector<DocletGroup>   doclet_groups;
        public MonitorableObject monitorable_object;
    }

    static public Result getDocletGroups(CallContext context, XMLNode xml_node, Origin origin) {
        DocletGroupXML dgx = new DocletGroupXML(context, xml_node, null, origin);
        if (dgx.getDoclets(context) != null) {
            dgx.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Root doclet container contains direct doclets '%(@container)'", "container", xml_node.toString(context)));
        }
        Result result = new Result();
        result.doclet_groups = dgx.getSubEntityDoclets(context);
        result.monitorable_object = dgx;
        return result;
    }

    protected Doclet processGroupNode(CallContext context, XMLNode group_node, Doclet parent_meta_data) {
        boolean have_entity = group_node.getAttribute(context, "Entity").length() != 0;
        String name = group_node.getName(context);
        if (have_entity) {
            if (name.matches("(?:docl:)?Group")) {
                String parent_aspect = parent_meta_data.getAspect(context);
                if (parent_aspect != null && parent_aspect.length() != 0) {
                    this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet groups for an entity (here: '%(entity)') must not be contained within aspects ('%(aspect)')", "entity", group_node.getAttribute(context, "Entity"), "aspect", parent_aspect));
                }
                if (this.sub_entities == null) {
                    this.sub_entities = new Vector<DocletGroup>();
                }
                this.sub_entities.add(new DocletGroupXML (context, group_node, parent_meta_data, this.origin));
            } else {
                this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet tags serve only as a container and must not contain any reference to an entity themselves (while their childs may): '%(@doclet)'", "doclet", group_node.toString(context)));
            }
            return null;
        } else {
            return this.processChilds(context, group_node, parent_meta_data);
        }
    }

    protected Doclet processChilds(CallContext context, XMLNode group_node, Doclet parent_meta_data) {
        DocletXML my_local_meta_data = new DocletXML(context, group_node, parent_meta_data, this.origin);
        my_local_meta_data.transferProblemsTo(context, this);
        for (XMLNode child : group_node.getChilds(context).getIterable(context)) {
            if (child.isText(context)) {
                if (child.toString(context).matches("^\\s*$") == false) {
                    this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet group contains non-empty text '%(text)'", "text", child.toString(context)));
                }
                // ok, ignored
            } else if (child.isElement(context)) {
                String name = child.getName(context);
                String namespace = child.getNamespace(context);
                if (name == null || name.length() == 0) {
                    this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet group contains invalid node (no simple name) '%(value)'", "value", child.toString(context)));
                } else {
                    if (namespace != null && namespace.equals("http://xmlns.sphenon.de/docl")) {
                        if (name.matches("(?:docl:)?(?:Group|Doclet)")) {
                            this.processGroupNode(context, child, my_local_meta_data);
                        } else {
                            if (this.doclets == null) {
                                this.doclets = new Vector<Doclet>();
                            }
                            DocletXML child_doclet = new DocletXML(context, child, my_local_meta_data, this.origin);
                            child_doclet.transferProblemsTo(context, this);
                            this.doclets.add(child_doclet);
//                             System.err.println("ADD DOC");
                        }
                    } else if (namespace == null || namespace.length() == 0) {
                        this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet group contains unexpected element '%(element)'", "element", child.toString(context)));
                    } else {
                        // ok, ignored
                    }
                }
            } else if (child.isComment(context)) {
                // ok, ignored
            } else if (child.isDocumentType(context)) {
                // ok, ignored
            } else if (child.isProcessingInstruction(context)) {
                // ok, ignored
            } else {
                this.addProblemStatus(context, ProblemState.ERROR, CustomaryContext.create((Context)context).createPreConditionViolation(context, "Doclet group contains invalid node of type '%(nodetype)' and value '%(value)'", "nodetype", child.getNodeType(context), "value", child.toString(context)));
            }
        }
        return my_local_meta_data;
    }

    public Vector<DocletGroup> getSubEntityDoclets(CallContext context) {
        return this.sub_entities;
    }

    public Vector<Doclet> getDoclets(CallContext context) {
        return this.doclets;
    }

    public String getEntity(CallContext context) {
        return this.local_meta_data.getEntity(context);
    }

    public String getEntityType(CallContext context) {
        return this.local_meta_data.getEntityType(context);
    }

    public String getEntityVersion(CallContext context) {
        return this.local_meta_data.getEntityVersion(context);
    }

    public String getSecurityClass(CallContext context) {
        return this.local_meta_data.getSecurityClass(context);
    }

    public String getAudience(CallContext context) {
        return this.local_meta_data.getAudience(context);
    }

    public String getLayout(CallContext context) {
        return this.local_meta_data.getLayout(context);
    }

    // copy from Class_OriginAware - Java no multiple inheritance

    protected Origin origin;

    public Origin getOrigin (CallContext context) {
        return this.origin;
    }

    public void setOrigin (CallContext context, Origin origin) {
        this.origin = origin;
    }
}
