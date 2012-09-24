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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SessionImpl implements Session {

    private final ProcessingEnvironment env;
    private final RoundEnvironment roundEnv;
    private final List<RootResourceBuilderImpl> resources = new ArrayList<RootResourceBuilderImpl>();
    private final Map<String, Set<RootResourceBuilderImpl>> rootByNamespace = new HashMap<String, Set<RootResourceBuilderImpl>>();

    SessionImpl(final ProcessingEnvironment env, final RoundEnvironment roundEnv) {
        this.env = env;
        this.roundEnv = roundEnv;
    }

    public RootResourceBuilder rootResource(final String type, final DeclaredType resourceInterface, final String version) {
        final RootResourceBuilderImpl builder = new RootResourceBuilderImpl(this, type, resourceInterface);
        resources.add(builder);
        return builder;
    }

    void addNamespace(String namespace, RootResourceBuilderImpl impl) {
        Set<RootResourceBuilderImpl> set = rootByNamespace.get(namespace);
        if (set == null) {
            rootByNamespace.put(namespace, set = new HashSet<RootResourceBuilderImpl>());
        }
        set.add(impl);
    }

    public AttributeTypeBuilder attributeType(final DeclaredType attributeInterface) {
        return null;
    }

    public Session addXmlNamespace(final String xmlns, final String version, final String schemaLocation) {
        return null;
    }

    public Session generateSource() {
        new Generator(env, roundEnv, this).generate();
        return this;
    }

    List<RootResourceBuilderImpl> getResources() {
        return resources;
    }
}
