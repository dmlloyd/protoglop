/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.mgmt.generator;

import org.jboss.jdeparser.JBlock;
import org.jboss.jdeparser.JConditional;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JMethod;
import org.jboss.jdeparser.JVar;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import static org.jboss.jdeparser.JMod.FINAL;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class PrimitiveAttributeValueInfo extends AttributeValueInfo {

    private final TypeKind kind;
    private final String name;
    private final VariableElement defaultVal;

    public PrimitiveAttributeValueInfo(final TypeKind kind, final String name, final VariableElement defaultVal, final boolean required) {
        this.kind = kind;
        this.name = name;
        this.defaultVal = defaultVal;
    }

    public void generate(final AttributeGeneratorContext attributeGeneratorContext) {
        JMethod setterDecl = null;
        JBlock setterBody = null;
        final JVar param;
        switch (kind) {
            case BOOLEAN: param = setterDecl.param(FINAL, boolean.class, name); break;
            case BYTE:    param = setterDecl.param(FINAL, byte.class,    name); break;
            case CHAR:    param = setterDecl.param(FINAL, char.class,    name); break;
            case DOUBLE:  param = setterDecl.param(FINAL, double.class,  name); break;
            case FLOAT:   param = setterDecl.param(FINAL, float.class,   name); break;
            case INT:     param = setterDecl.param(FINAL, int.class,     name); break;
            case LONG:    param = setterDecl.param(FINAL, long.class,    name); break;
            case SHORT:   param = setterDecl.param(FINAL, short.class,   name); break;
            default: throw new IllegalStateException();
        }
        if (defaultVal != null) {
            // todo all wrong
            final JConditional _if = setterBody._if(param.eq(JExpr._null()));
            _if._then().assign(JExpr._this().ref(name), JExpr.ref(defaultVal.getSimpleName().toString()));
            _if._else().assign(JExpr._this().ref(name), param);
            setterBody._return(JExpr._this());
        } else {
            setterBody.assign(JExpr._this().ref(name), param);
            setterBody._return(JExpr._this());
        }
    }

    public boolean isValidInAttributeType() {
        return true;
    }
}
