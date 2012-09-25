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

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import javax.annotation.processing.Filer;

import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * A bridge between {@link CodeWriter} and {@link Filer}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class FilerCodeWriter extends CodeWriter {

    private final Filer filer;

    FilerCodeWriter(final Filer filer) {
        this.filer = filer;
    }

    public OutputStream openBinary(final JPackage pkg, final String fileName) throws IOException {
        final FileObject sourceFile;
        if (fileName.endsWith(".java")) {
            sourceFile = filer.createSourceFile(pkg.name() + "." + fileName.substring(0, fileName.length() - 5));
        } else {
            sourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT, pkg.name(), fileName);
        }
        return sourceFile.openOutputStream();
    }

    public void close() throws IOException {
    }
}
