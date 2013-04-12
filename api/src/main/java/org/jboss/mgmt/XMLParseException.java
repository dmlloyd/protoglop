/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.mgmt;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * An XML parse exception with a human-readable location.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class XMLParseException extends XMLStreamException {

    private static final long serialVersionUID = 4867819352898332932L;

    /**
     * Constructs a {@code XMLParseException} with no detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public XMLParseException() {
        this(XMLParseLocation.UNKNOWN);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public XMLParseException(final String msg) {
        this(msg, XMLParseLocation.UNKNOWN);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public XMLParseException(final Throwable cause) {
        this(XMLParseLocation.UNKNOWN, cause);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public XMLParseException(final String msg, final Throwable cause) {
        this(msg, XMLParseLocation.UNKNOWN, cause);
    }

    /**
     * Constructs a {@code XMLParseException} with no detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public XMLParseException(final XMLParseLocation location) {
        this(location, null);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public XMLParseException(final String msg, final XMLParseLocation location) {
        this(msg, location, null);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public XMLParseException(final XMLParseLocation location, final Throwable cause) {
        this("Parse error", location, cause);
    }

    /**
     * Constructs a {@code XMLParseException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public XMLParseException(final String msg, final XMLParseLocation location, final Throwable cause) {
        super(location.toString() + msg, cause);
        this.location = location;
    }

    /**
     * Constructs a {@code XMLParseException} using information gleaned from the given stream exception.
     *
     * @param streamException the exception to acquire information from
     * @param fileName the file name to use in the location, if any
     */
    public XMLParseException(final XMLStreamException streamException, final String fileName) {
        this(clean(streamException.getMessage()), streamException.getLocation() == null ? XMLParseLocation.UNKNOWN : new XMLParseLocation(fileName, streamException.getLocation()), streamException.getCause());
    }

    private static String clean(String original) {
        if (original.startsWith("ParseError at [row,col]:[")) {
            return original.substring(original.indexOf("Message: ") + 9);
        } else {
            return original;
        }
    }

    public Throwable fillInStackTrace() {
        return this;
    }
}
