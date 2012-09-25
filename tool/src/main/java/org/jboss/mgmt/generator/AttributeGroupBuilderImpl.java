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

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeGroupBuilderImpl<P> implements AttributeGroupBuilder<P> {
    private final P parent;
    private final String name;
    private final DeclaredType type;
    private final List<AttributeBuilderImpl<?>> attributes = new ArrayList<AttributeBuilderImpl<?>>();

    public AttributeGroupBuilderImpl(final P parent, final String name, final DeclaredType type) {
        this.parent = parent;
        this.name = name;
        this.type = type;
    }

    public AttributeBuilder<AttributeGroupBuilder<P>> attribute(final String name) {
        final AttributeBuilderImpl<AttributeGroupBuilder<P>> attr = new AttributeBuilderImpl<AttributeGroupBuilder<P>>(this, name);
        attributes.add(attr);
        return attr;
    }

    public P end() {
        return parent;
    }

    String getName() {
        return name;
    }

    DeclaredType getType() {
        return type;
    }

    List<AttributeBuilderImpl<?>> getAttributes() {
        return attributes;
    }
}
