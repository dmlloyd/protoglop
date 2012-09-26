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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import nu.xom.Element;
import org.jboss.mgmt.annotation.Schema;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SchemaInfo {
    private final Map<String, Element> typeDecls = new TreeMap<String, Element>();
    private final Map<String, Element> rootElementDecls = new TreeMap<String, Element>();
    private final Set<String> altXmlNamespaces = new HashSet<String>();
    private final Schema schema;
    private final String xmlNamespace;

    SchemaInfo(final Schema schema) {
        this.schema = schema;
        xmlNamespace = GeneratorUtils.buildNamespace(schema.kind(), schema.namespace(), schema.version());
    }

    public Map<String, Element> getTypeDecls() {
        return typeDecls;
    }

    public Map<String, Element> getRootElementDecls() {
        return rootElementDecls;
    }

    public Set<String> getAltXmlNamespaces() {
        return altXmlNamespaces;
    }

    public Schema getSchema() {
        return schema;
    }

    public String getXmlNamespace() {
        return xmlNamespace;
    }
}
