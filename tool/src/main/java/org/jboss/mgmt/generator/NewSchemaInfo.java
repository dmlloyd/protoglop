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

import java.net.URI;
import java.net.URISyntaxException;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import org.jboss.mgmt.annotation.Schema;

import javax.annotation.processing.Messager;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewSchemaInfo {
    private static final String XSD = "http://www.w3.org/2001/XMLSchema";

    private final String version;
    private final String namespace;
    private final Schema.Kind kind;
    private final String schemaLocation;
    private final String schemaFileName;
    private final String[] compatNamespaces;

    private final RootResourceInfo[] resources;
    private final boolean localSource;
    private final String xmlNamespace;

    NewSchemaInfo(final String version, final String namespace, final Schema.Kind kind, final String schemaLocation, final String schemaFileName, final String[] compatNamespaces, final RootResourceInfo[] resources, final boolean localSource, final String xmlNamespace) {
        this.version = version;
        this.namespace = namespace;
        this.xmlNamespace = namespace;
        this.kind = kind;
        this.schemaLocation = schemaLocation;
        this.schemaFileName = schemaFileName;
        this.compatNamespaces = compatNamespaces;
        this.resources = resources;
        this.localSource = localSource;
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

    public boolean isLocalSource() {
        return localSource;
    }

    public void generate(final GeneratorContext genCtxt) {
        final Element schemaElement = new Element("xs:schema", XSD);
        final Document document = new Document(schemaElement);
        final String xmlNamespace = getXmlNamespace();
        final SchemaGeneratorContext ctxt = new SchemaGeneratorContext(genCtxt, this);

        schemaElement.addNamespaceDeclaration("", xmlNamespace);
        schemaElement.addNamespaceDeclaration("xs", XSD);

        schemaElement.addAttribute(new Attribute("targetNamespace", xmlNamespace));
        schemaElement.addAttribute(new Attribute("elementFormDefault", "qualified"));
        schemaElement.addAttribute(new Attribute("attributeFormDefault", "unqualified"));

        for (RootResourceInfo resourceInfo : resources) {
            resourceInfo.generate(ctxt);
        }
        schemaElement.appendChild(new Comment("\nRoot elements\n"));
        for (Element element : ctxt.getRootElements()) {
            schemaElement.appendChild(element);
        }
        schemaElement.appendChild(new Comment("\nElement types\n"));
        for (Element element : ctxt.getRootTypes()) {
            schemaElement.appendChild(element);
        }

        if (! genCtxt.addDocument(this, document)) {
            genCtxt.getEnv().getMessager().printMessage(ERROR, "Schema info " + this + " generated more than once");
        }
    }

    public static String fileNameFromSchemaLocation(Messager messager, String schemaLocation, String xmlNamespace) {
        if (schemaLocation == null) {
            messager.printMessage(ERROR, "No namespace location for schema " + xmlNamespace);
            return null;
        }
        final URI uri;
        try {
            uri = new URI(schemaLocation);
        } catch (URISyntaxException e) {
            messager.printMessage(ERROR, "Namespace schema location '" + schemaLocation + "' is not valid for " + xmlNamespace);
            return null;
        }
        final String path = uri.getPath();
        if (path == null) {
            messager.printMessage(ERROR, "Namespace schema location '" + schemaLocation + "' does not have a path component for " + xmlNamespace);
            return null;
        }
        final String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (! fileName.endsWith(".xsd")) {
            messager.printMessage(WARNING, "Namespace schema location '" + schemaLocation + "' should specify a file name ending in \".xsd\"");
        }
        return fileName;
    }

    public String getXmlNamespace() {
        return xmlNamespace;
    }
}
