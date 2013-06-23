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
abstract class AttributeValueInfo {
    private final String name;

    AttributeValueInfo(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean isValidInAttributeType();

    public abstract void addToSchemaAsAttribute(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element attributeElement);

    public abstract void addToSchemaAsElement(final AttributeInfo attributeInfo, final Element enclosingSeqElement, final Element enclosingTypeElement, final Element elementElement);

    public abstract void addToResourceClass(JDefinedClass resourceClass, JMethod constructor);

    public abstract void addToBuilderClass(JDefinedClass builderClass);

    public abstract void addToResolvedResourceClass(JDefinedClass resolvedClass, JMethod constructor);

    public abstract void addToResolvedInterface(JDefinedClass resolvedInterface);
}
