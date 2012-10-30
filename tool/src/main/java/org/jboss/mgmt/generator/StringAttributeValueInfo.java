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
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JFieldRef;
import org.jboss.jdeparser.JMethod;
import org.jboss.jdeparser.JType;

import javax.lang.model.element.VariableElement;

import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PRIVATE;
import static org.jboss.jdeparser.JMod.PUBLIC;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StringAttributeValueInfo extends AttributeValueInfo {

    private final String name;
    private final VariableElement defaultVal;
    private final boolean required;
    private final String[] enumerations;

    public StringAttributeValueInfo(final String name, final VariableElement defaultVal, final boolean required, final String[] enumerations) {
        this.name = name;
        this.defaultVal = defaultVal;
        this.required = required;
        this.enumerations = enumerations;
    }

    public String getName() {
        return name;
    }

    public VariableElement getDefaultVal() {
        return defaultVal;
    }

    public boolean isRequired() {
        return required;
    }

    public String[] getEnumerations() {
        return enumerations;
    }

    public void generate(final AttributeGeneratorContext attributeGeneratorContext) {
        final ResourceGeneratorContext resourceGeneratorContext = attributeGeneratorContext.getResourceGeneratorContext();
        final JDefinedClass resourceImplClass = resourceGeneratorContext.getResourceImplClass();
        final JMethod implConstructor = resourceGeneratorContext.getResourceImplConstructor();
        final JBlock implInitBlock = resourceGeneratorContext.getResourceImplConstructorInitBlock();
        final JType attributeType = attributeGeneratorContext.getAttributeType();
        final String attributeName = attributeGeneratorContext.getAttributeInfo().getName();
        // init
        final JFieldRef implField = JExpr.ref(JExpr._this(), resourceImplClass.field(PRIVATE | FINAL, attributeType, attributeName));
        implInitBlock.assign(implField, implConstructor.param(FINAL, attributeType, attributeName));
        // getter
        final JMethod getterMethod = resourceImplClass.method(PUBLIC, attributeType, attributeGeneratorContext.getGetterName());
        getterMethod.body()._return(implField);
    }

    public boolean isValidInAttributeType() {
        return true;
    }
}
