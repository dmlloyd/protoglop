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

import nu.xom.Element;
import org.jboss.mgmt.annotation.Schema;

import com.sun.codemodel.JDefinedClass;

import javax.annotation.Generated;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class GeneratorUtils {

    private GeneratorUtils() {
    }

    static final String XSD = "http://www.w3.org/2001/XMLSchema";
    static final String GENERATOR_VERSION = "1.0";

    static <T> T def(T test, T def) {
        return test == null ? def : test;
    }

    static String without(String name, String suffix) {
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    static String singular(String plural) {
        if (plural.endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        } else if (plural.endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        } else {
            return plural;
        }
    }

    /*
      foo-bar-a-baz -> FooBarABaz
     */
    static String classify(String xmlName) {
        boolean up = true;
        final int length = xmlName.length();
        final StringBuilder builder = new StringBuilder(xmlName.length());
        int idx = 0;
        while (idx < length) {
            int c = xmlName.codePointAt(idx);
            idx = xmlName.offsetByCodePoints(idx, 1);
            if (c == '-') {
                up = true;
            } else {
                builder.appendCodePoint(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return builder.toString();
    }

    /*
      AFunnyXMLKindAThing -> a-funny-xml-kind-a-thing
     */
    static String xmlify(String camelHumpsName) {
        final int length = camelHumpsName.length();
        final StringBuilder builder = new StringBuilder(length + length >> 1);
        int idx = 0;
        int c = camelHumpsName.codePointAt(idx), n;
        boolean wordDone = false;
        for (;;) {
            idx = camelHumpsName.offsetByCodePoints(idx, 1);
            if (idx < length) {
                n = camelHumpsName.codePointAt(idx);
                if (Character.isLowerCase(c) && Character.isUpperCase(n)) {
                    builder.appendCodePoint(c);
                    wordDone = true;
                } else if (builder.length() > 0 && Character.isUpperCase(c) && Character.isLowerCase(n) || wordDone) {
                    builder.append('-');
                    builder.appendCodePoint(Character.toLowerCase(c));
                    wordDone = false;
                } else {
                    builder.appendCodePoint(Character.toLowerCase(c));
                }
                c = n;
                continue;
            } else {
                builder.appendCodePoint(Character.toLowerCase(c));
                return builder.toString();
            }
        }
    }

    /*
      AFunnyXMLKindAThing -> A_FUNNY_XML_KIND_A_THING
     */
    static String constify(String camelHumpsName) {
        final int length = camelHumpsName.length();
        final StringBuilder builder = new StringBuilder(length + length >> 1);
        int idx = 0;
        int c = camelHumpsName.codePointAt(idx), n;
        boolean wordDone = false;
        for (;;) {
            idx = camelHumpsName.offsetByCodePoints(idx, 1);
            if (idx < length) {
                n = camelHumpsName.codePointAt(idx);
                if (Character.isLowerCase(c) && Character.isUpperCase(n)) {
                    builder.appendCodePoint(Character.toUpperCase(c));
                    wordDone = true;
                } else if (builder.length() > 0 && Character.isUpperCase(c) && Character.isLowerCase(n) || wordDone) {
                    builder.append('_');
                    builder.appendCodePoint(Character.toUpperCase(c));
                    wordDone = false;
                } else {
                    builder.appendCodePoint(Character.toUpperCase(c));
                }
                c = n;
                continue;
            } else {
                builder.appendCodePoint(Character.toUpperCase(c));
                return builder.toString();
            }
        }
    }

    /*
      BLAFunnyJavaKindAThing -> blaFunnyJavaKindAThing
      FOO -> foo
      Foo -> foo
      foo -> foo
      FOOBar -> fooBar
     */
    static String fieldify(String camelHumpsName) {
        final int length = camelHumpsName.length();
        final StringBuilder builder = new StringBuilder(length);
        int idx = 0;
        int c = camelHumpsName.codePointAt(idx), n;
        if (Character.isLowerCase(c)) {
            return camelHumpsName;
        }
        builder.appendCodePoint(Character.toLowerCase(c));
        idx = camelHumpsName.offsetByCodePoints(idx, 1);
        c = camelHumpsName.codePointAt(idx);
        for (;;) {
            idx = camelHumpsName.offsetByCodePoints(idx, 1);
            if (idx < length) {
                n = camelHumpsName.codePointAt(idx);
                if (Character.isLowerCase(n)) {
                    // next is lowercase; we're done
                    builder.appendCodePoint(c);
                    builder.append(camelHumpsName.substring(idx));
                    return builder.toString();
                } else {
                    builder.appendCodePoint(Character.toLowerCase(c));
                    c = n;
                }
            } else {
                builder.appendCodePoint(Character.toLowerCase(c));
                return builder.toString();
            }
        }
    }

    static String buildNamespace(final Schema.Kind kind, final String namespace, final String version) {
        StringBuilder b = new StringBuilder(64);
        if (kind == Schema.Kind.SYSTEM) {
            b.append("sys:");
        } else {
            b.append("ext:");
        }
        b.append(namespace).append(':').append(version);
        return b.toString();
    }

    static void addDocumentation(final Element element, final String documentation) {
        final Element annotation = new Element("xs:annotation", XSD);
        element.appendChild(annotation);
        final Element documentationElement = new Element("xs:documentation", XSD);
        annotation.appendChild(documentationElement);
        documentationElement.appendChild(documentation);
    }

    static void addGeneratedAnnotation(final JDefinedClass target) {
        target.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
    }
}
