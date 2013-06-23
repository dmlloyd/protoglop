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

import javax.annotation.Generated;

import static org.wildfly.core.management.processor.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class GeneratorUtils {

    private GeneratorUtils() {
    }

    static final String GENERATOR_VERSION = "1.0";

    static <T> T def(T test, T def) {
        return test == null ? def : test;
    }

    static void addDocumentation(final Element element, final String documentation) {
        final Element annotation = new Element("xs:annotation", SchemaInfo.XS);
        element.appendChild(annotation);
        final Element documentationElement = new Element("xs:documentation", SchemaInfo.XS);
        annotation.appendChild(documentationElement);
        documentationElement.appendChild(documentation);
    }

    static void addGeneratedAnnotation(final JDefinedClass target) {
        target.annotate(Generated.class).paramArray("value").param(Processor.class.getName()).param(GENERATOR_VERSION);
    }
}
