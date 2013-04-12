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

import static java.lang.Integer.signum;

/**
 * An XML location which is readable by humans.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class XMLParseLocation implements Location, Comparable<XMLParseLocation> {

    /**
     * An unknown parse location.
     */
    public static final XMLParseLocation UNKNOWN = new XMLParseLocation(null, -1, -1, -1);

    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;
    private final int characterOffset;
    private final String publicId;
    private final String systemId;

    /**
     * Construct a new instance.
     *
     * @param fileName the file name (may be {@code null} if this location does not correspond to a file)
     * @param lineNumber the line number (may be {@code -1} if not known)
     * @param columnNumber the column number (may be {@code -1} if not known)
     * @param characterOffset the character offset (may be {@code -1} if not known)
     * @param publicId the XML public ID (may be {@code null})
     * @param systemId the XML system ID (may be {@code null})
     */
    public XMLParseLocation(final String fileName, final int lineNumber, final int columnNumber, final int characterOffset, final String publicId, final String systemId) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.characterOffset = characterOffset;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    /**
     * Construct a new instance.
     *
     * @param fileName the file name (may be {@code null} if this location does not correspond to a file)
     * @param lineNumber the line number (may be {@code -1} if not known)
     * @param columnNumber the column number (may be {@code -1} if not known)
     * @param characterOffset the character offset (may be {@code -1} if not known)
     */
    public XMLParseLocation(final String fileName, final int lineNumber, final int columnNumber, final int characterOffset) {
        this(fileName, lineNumber, columnNumber, characterOffset, null, null);
    }

    /**
     * Construct a new instance.
     *
     * @param fileName the file name (may be {@code null} if this location does not correspond to a file)
     * @param original the location to copy the remainder of the information from
     */
    public XMLParseLocation(final String fileName, final Location original) {
        this(fileName, original.getLineNumber(), original.getColumnNumber(), original.getCharacterOffset(), original.getPublicId(), original.getSystemId());
    }

    /**
     * Construct a new instance.
     *
     * @param original the location to copy the remainder of the information from
     */
    public XMLParseLocation(final Location original) {
        this(original instanceof XMLParseLocation ? ((XMLParseLocation)original).getFileName() : null, original.getLineNumber(), original.getColumnNumber(), original.getCharacterOffset(), original.getPublicId(), original.getSystemId());
    }

    /**
     * Get the file name.  May be {@code null} if this location does not correspond to a file.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the line number where the corresponding event ends.  Returns -1 if not known.
     *
     * @return the line number where the corresponding event ends
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number where the corresponding event ends.  Returns -1 if not known.
     *
     * @return the column number where the corresponding event ends
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Get the absolute character offset of this event.  Returns -1 if not known.
     *
     * @return the absolute character offset of this event
     */
    public int getCharacterOffset() {
        return characterOffset;
    }

    /**
     * Get the public ID of the XML.  Returns {@code null} if not known.
     *
     * @return the public ID of the XML
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Get the system ID of the XML.  Returns {@code null} if not known.
     *
     * @return the system ID of the XML
     */
    public String getSystemId() {
        return systemId;
    }

    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + lineNumber;
        result = 31 * result + columnNumber;
        result = 31 * result + characterOffset;
        result = 31 * result + (publicId != null ? publicId.hashCode() : 0);
        result = 31 * result + (systemId != null ? systemId.hashCode() : 0);
        return result;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof XMLParseLocation && equals((XMLParseLocation)other);
    }

    private static boolean equals(Object a, Object b) {
        return a == b || a == null ? b == null : a.equals(b);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(XMLParseLocation other) {
        return this == other || other != null && equals(fileName, other.fileName) && lineNumber == other.lineNumber && columnNumber == other.columnNumber && characterOffset == other.characterOffset && equals(publicId, other.publicId) && equals(systemId, other.systemId);
    }

    /**
     * Get the location as a string.  The string will be suitable for immediately prefixing an error message.
     *
     * @return the location as a string
     */
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(fileName == null ? "<input>" : fileName).append(':');
        if (lineNumber > 0) {
            b.append(lineNumber).append(':');
            if (columnNumber > 0) {
                b.append(columnNumber).append(':');
            }
        }
        b.append(' ');
        return b.toString();
    }

    private int compareString(String a, String b) {
        return a == null ? b == null ? 0 : 1 : b == null ? -1 : a.compareTo(b);
    }

    /**
     * Compare for sort.
     *
     * @param o the other location
     * @return the sort result (-1, 0, or 1)
     */
    public int compareTo(final XMLParseLocation o) {
        int c;
        c = compareString(fileName, o.fileName);
        if (c == 0) {
            c = signum(lineNumber - o.lineNumber);
            if (c == 0) {
                c = signum(columnNumber - o.columnNumber);
                if (c == 0) {
                    c = signum(characterOffset - o.characterOffset);
                }
            }
        }
        return c;
    }
}
