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

import nu.xom.Element;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JMethod;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ReferenceAttributeValueInfo extends AttributeValueInfo {

    private final ExecutableElement declaringElement;
    private final boolean required;
    private final DeclaredType referenceType;
    private final boolean monitor;
    private final boolean list;
    private final DeclaredType targetType;

    public ReferenceAttributeValueInfo(final String name, final ExecutableElement declaringElement, final boolean required, final DeclaredType referenceType, final boolean monitor, final boolean list, final DeclaredType targetType) {
        super(name);
        this.declaringElement = declaringElement;
        this.required = required;
        this.referenceType = referenceType;
        this.monitor = monitor;
        this.list = list;
        this.targetType = targetType;
    }

    public void addToSchemaAsAttribute(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element attributeElement) {
    }

    public void addToSchemaAsElement(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element elementElement) {
    }

    public void addToResourceClass(final JDefinedClass resourceClass, final JMethod constructor) {
    }

    public void addToBuilderClass(final JDefinedClass builderClass) {
    }

    public void addToResolvedResourceClass(final JDefinedClass resolvedClass, final JMethod constructor) {
    }

    public void addToResolvedInterface(final JDefinedClass resolvedInterface) {
    }

    public ExecutableElement getDeclaringElement() {
        return declaringElement;
    }

    public boolean isRequired() {
        return required;
    }

    public DeclaredType getReferenceType() {
        return referenceType;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public boolean isList() {
        return list;
    }

    public DeclaredType getTargetType() {
        return targetType;
    }

    public boolean isValidInAttributeType() {
        return true;
    }
}
