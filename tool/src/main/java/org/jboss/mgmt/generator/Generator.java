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

import java.util.Locale;
import org.jboss.mgmt.AttributeListener;
import org.jboss.mgmt.annotation.RuntimeMode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Generator {

    private Generator() {
    }

    private static void EXAMPLE() {
        // Annotation processor reads the data and then does this
        openSession()
            .model("domain")
                .resource("bar")
                    .attribute()
                        .name("blah")
                        .done()
                    .provides("blah")
                    .done()
                .resource("foo")
                    .subResource("blah", true)
                    .attribute()
                        .name("zap")
                        .description(Locale.ROOT, "Blah blah blah")
                        .description(Locale.US, "Yo blah blah blah")
                        .listener(AttributeListener.class, RuntimeMode.HOST, RuntimeMode.MANAGEMENT)
                        .done()
                    .done()
                .done()
            .done()
        .generateSource("target/generated-sources/whatever");
    }

    public static Session openSession() {
        return null;
    }
}
