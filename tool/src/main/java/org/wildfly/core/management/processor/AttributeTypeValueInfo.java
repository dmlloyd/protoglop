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

package org.wildfly.core.management.processor;

import nu.xom.Element;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JMethod;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeTypeValueInfo extends AttributeValueInfo {

    public AttributeTypeValueInfo(final String name, final AttributeTypeInfo attributeTypeInfo, final boolean required) {
        super(name);
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

    public boolean isValidInAttributeType() {
        return true;
    }
}
