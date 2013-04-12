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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import nu.xom.Document;
import nu.xom.Serializer;

import org.jboss.jdeparser.JDeparser;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class GeneratorContext {

    private final ProcessingContext ctxt;
    private final JDeparser deparser = new JDeparser();

    private final Map<SchemaInfo, Document> documents = new IdentityHashMap<SchemaInfo, Document>();
    private final Set<Object> generatedClassInfos = new HashSet<Object>();

    GeneratorContext(final ProcessingContext ctxt) {
        this.ctxt = ctxt;
    }

    public JDeparser getDeparser() {
        return deparser;
    }

    public boolean addDocument(SchemaInfo info, Document document) {
        return ! documents.containsKey(info) && documents.put(info, document) == null;
    }

    public void generate() {
        final RoundEnvironment roundEnv = ctxt.getRoundEnv();
        final Messager messager = ctxt.getEnv().getMessager();
        final Filer filer = ctxt.getEnv().getFiler();
        for (SchemaInfo schema : ctxt.getSchemas()) {
            if (schema.isLocalSource()) {
                final SchemaGeneratorContext schemaGeneratorContext = new SchemaGeneratorContext(this, schema);
                schemaGeneratorContext.generate();
            }
        }
        if (! roundEnv.errorRaised()) {
            try {
                deparser.build(new FilerCodeWriter(filer));
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate code model: " + e);
            }
        }
        for (Map.Entry<SchemaInfo, Document> entry : documents.entrySet()) {
            if (! roundEnv.errorRaised()) {
                final SchemaInfo schemaInfo = entry.getKey();
                final Document document = entry.getValue();
                final Serializer serializer;
                final OutputStream stream;
                final String fileName = schemaInfo.getSchemaFileName();
                final String schemaLocation = schemaInfo.getSchemaLocation();
                final String xmlNamespace = schemaInfo.getXmlNamespace();
                try {
                    stream = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "META-INF/" + fileName).openOutputStream();
                    try {
                        serializer = new NiceSerializer(stream);
                        serializer.setIndent(4);
                        serializer.setLineSeparator("\n");
                        serializer.write(document);
                        serializer.flush();
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            messager.printMessage(ERROR, "Failed to close XSD stream for '" + schemaLocation + "' of " + xmlNamespace + ": " + e);
                        }
                    }
                } catch (IOException e) {
                    messager.printMessage(ERROR, "Failed to write XSD for '" + schemaLocation + "' of " + xmlNamespace + ": " + e);
                }
            } else {
                break;
            }
        }
    }

    public ProcessingEnvironment getEnv() {
        return ctxt.getEnv();
    }

    public boolean generated(final Object info) {
        return ! generatedClassInfos.add(info);
    }

    static class NiceSerializer extends Serializer {

        public NiceSerializer(OutputStream out) {
            super(out);
        }

        protected void writeXMLDeclaration() throws IOException {
            super.writeXMLDeclaration();
            super.breakLine();
        }
    }
}
