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

import org.jboss.mgmt.annotation.Schema;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewSchemaInfo {
    private final String version;
    private final String namespace;
    private final Schema.Kind kind;
    private final String schemaLocation;
    private final String schemaFileName;
    private final String[] compatNamespaces;

    private final RootResourceInfo[] resources;

    NewSchemaInfo(final String version, final String namespace, final Schema.Kind kind, final String schemaLocation, final String schemaFileName, final String[] compatNamespaces, final RootResourceInfo[] resources) {
        this.version = version;
        this.namespace = namespace;
        this.kind = kind;
        this.schemaLocation = schemaLocation;
        this.schemaFileName = schemaFileName;
        this.compatNamespaces = compatNamespaces;
        this.resources = resources;
    }

    public String getVersion() {
        return version;
    }

    public String getNamespace() {
        return namespace;
    }

    public Schema.Kind getKind() {
        return kind;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public String getSchemaFileName() {
        return schemaFileName;
    }

    public String[] getCompatNamespaces() {
        return compatNamespaces;
    }

    public RootResourceInfo[] getResources() {
        return resources;
    }
}
