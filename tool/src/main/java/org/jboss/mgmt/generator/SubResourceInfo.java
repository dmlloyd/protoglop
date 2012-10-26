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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SubResourceInfo implements ResourceMember {

    private final ExecutableElement collector;
    private final ExecutableElement fetcher;
    private final TypeMirror resourceType;
    private final String type;
    private final String name;
    private final boolean requiresUnique;
    private final ResourceInfo[] knownChildren;

    public SubResourceInfo(final ExecutableElement collector, final ExecutableElement fetcher, final TypeMirror resourceType, final String type, final String name, final boolean requiresUnique, final ResourceInfo[] knownChildren) {
        this.collector = collector;
        this.fetcher = fetcher;
        this.type = type;
        this.name = name;
        this.requiresUnique = requiresUnique;
        this.knownChildren = knownChildren;
        this.resourceType = resourceType;
    }

    public void generate(final ResourceGeneratorContext resourceGeneratorContext) {
        final ResourceGeneratorContext childContext = new ResourceGeneratorContext(resourceGeneratorContext.getContext(), resourceGeneratorContext.getResourceInfo(), resourceGeneratorContext);
        for (ResourceInfo resourceInfo : knownChildren) {
            resourceInfo.generate(childContext);
        }
    }
}
