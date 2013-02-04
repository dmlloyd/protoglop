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

import nu.xom.Attribute;
import nu.xom.Element;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JFieldVar;
import org.jboss.jdeparser.JMethod;
import org.jboss.jdeparser.JVar;

import javax.lang.model.element.VariableElement;

import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PRIVATE;
import static org.jboss.jdeparser.JMod.PUBLIC;
import static org.jboss.mgmt.generator.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StringAttributeValueInfo extends AttributeValueInfo {

    private final String name;
    private final VariableElement defaultVal;
    private final boolean required;
    private final String[] enumerations;

    public StringAttributeValueInfo(final String name, final VariableElement defaultVal, final boolean required, final String[] enumerations) {
        super(name);
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

    public boolean isValidInAttributeType() {
        return true;
    }

    public void addToResourceClass(JDefinedClass resourceClass, JMethod constructor) {
        final String fieldName = NameUtils.fieldify(name);
        final JFieldVar field = resourceClass.field(PRIVATE | FINAL, String.class, fieldName);
        final JVar constructorParam = constructor.param(String.class, fieldName);
        constructor.body().assign(JExpr._this().ref(field), constructorParam);

        final JMethod getter = resourceClass.method(PUBLIC, String.class, "get" + name);
        getter.body()._return(field);
    }

    public void addToResolvedResourceClass(JDefinedClass resolvedClass, JMethod constructor) {
        addToResourceClass(resolvedClass, constructor);
    }

    public void addToBuilderClass(JDefinedClass builderClass) {
        final String varName = NameUtils.fieldify(name);
        builderClass.field(PRIVATE, String.class, varName);
        final JMethod setter = builderClass.method(PUBLIC | FINAL, builderClass, "set" + name);
        final JVar valParam = setter.param(FINAL, String.class, varName);
        setter.body().assign(JExpr._this().ref(varName), valParam);
        setter.body()._return(JExpr._this());
    }

    public void addToResolvedInterface(JDefinedClass resolvedInterface) {
        resolvedInterface.method(0, String.class, "get" + name);
    }

    public void addToSchemaAsAttribute(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element attributeElement) {
        attributeElement.addAttribute(new Attribute("type", "xs:string"));
        if (defaultVal != null) {
            attributeElement.addAttribute(new Attribute("default", defaultVal.getConstantValue().toString()));
        }
    }

    public void addToSchemaAsElement(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element elementElement) {
        final Element complexType = new Element("xs:complexType", XS);
        elementElement.appendChild(complexType);
        final Element attributeElement = new Element("xs:attribute", XS);
        complexType.appendChild(attributeElement);
        attributeElement.addAttribute(new Attribute("name", "value"));
        attributeElement.addAttribute(new Attribute("type", "xs:string"));
    }
}
