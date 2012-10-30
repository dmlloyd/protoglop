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

import com.sun.codemodel.JType;

import javax.lang.model.type.TypeKind;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeGeneratorContext {

    private final AttributeInfo attributeInfo;
    private final ResourceGeneratorContext resourceGeneratorContext;
    private final JType attributeType;
    private final String getterName;

    public AttributeGeneratorContext(final AttributeInfo attributeInfo, final ResourceGeneratorContext resourceGeneratorContext) {
        this.attributeInfo = attributeInfo;
        this.resourceGeneratorContext = resourceGeneratorContext;
        final GeneratorContext generatorContext = resourceGeneratorContext.getContext().getContext();
        attributeType = CodeModelUtils.typeFor(generatorContext.getEnv(), generatorContext.getCodeModel(), attributeInfo.getExecutableElement().getReturnType());
        if (attributeType.equals(resourceGeneratorContext.getContext().getContext().getEnv().getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN))) {
            getterName = "is" + attributeInfo.getName();
        } else {
            getterName = "get" + attributeInfo.getName();
        }
    }

    public AttributeInfo getAttributeInfo() {
        return attributeInfo;
    }

    public ResourceGeneratorContext getResourceGeneratorContext() {
        return resourceGeneratorContext;
    }

    public JType getAttributeType() {
        return attributeType;
    }

    public String getGetterName() {
        return getterName;
    }
}
