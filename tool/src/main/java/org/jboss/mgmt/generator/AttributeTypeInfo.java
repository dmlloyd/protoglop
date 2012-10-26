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

import org.jboss.mgmt.annotation.XmlRender;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeTypeInfo {

    private final TypeElement typeElement;
    private final DeclaredType[] validators;
    private final AttributeInfo[] attributes;
    private final String implClassName;
    private final XmlRender.As renderAs;
    private final String xmlTypeName;
    private final boolean wrapperElement;

    AttributeTypeInfo(final TypeElement typeElement, final String implClassName, final String xmlTypeName, final XmlRender.As renderAs, final boolean wrapperElement, final DeclaredType[] validators, final AttributeInfo[] attributes) {
        this.typeElement = typeElement;
        this.implClassName = implClassName;
        this.xmlTypeName = xmlTypeName;
        this.renderAs = renderAs;
        this.wrapperElement = wrapperElement;
        this.validators = validators;
        this.attributes = attributes;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public DeclaredType[] getValidators() {
        return validators;
    }

    public String getImplClassName() {
        return implClassName;
    }

    public XmlRender.As getRenderAs() {
        return renderAs;
    }

    public String getXmlTypeName() {
        return xmlTypeName;
    }

    public boolean isWrapperElement() {
        return wrapperElement;
    }
}
