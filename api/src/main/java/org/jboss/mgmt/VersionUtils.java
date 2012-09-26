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

package org.jboss.mgmt;

import java.util.Comparator;
import java.util.regex.Pattern;

import static java.lang.Integer.signum;
import static java.lang.Math.min;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class VersionUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");

    private VersionUtils() {
    }

    /**
     * Determine whether a given version string is valid.
     *
     * @param versionString the version string
     * @return {@code true} if it is valid, {@code false} otherwise
     */
    public static boolean isVersionValid(String versionString) {
        return VERSION_PATTERN.matcher(versionString).matches();
    }

    private static int[] versionToSegments(String versionString, int idx, int i) {
        int c, d, a;
        c = versionString.codePointAt(idx);
        idx = versionString.offsetByCodePoints(idx, 1);
        d = Character.digit(c, 10);
        if (d == -1) {
            throw new IllegalArgumentException("String is not in version format");
        }
        a = d;
        for (;;) {
            c = versionString.codePointAt(idx);
            idx = versionString.offsetByCodePoints(idx, 1);
            if (c == '.') {
                final int[] ints = versionToSegments(versionString, idx + 1, i + 1);
                ints[i] = a;
                return ints;
            }
            d = Character.digit(c, 10);
            if (d == -1) {
                throw new IllegalArgumentException("String is not in version format");
            }
            a = a * 10 + d;
            if (idx == versionString.length()) {
                final int[] ints = new int[i + 1];
                ints[i] = a;
                return ints;
            }
        }
    }

    public static String segmentsToVersion(int[] segments) {
        final StringBuilder b = new StringBuilder(segments.length * 3);
        for (int idx = 0; idx < segments.length; idx++) {
            b.append(segments[idx]);
            if (idx < segments.length - 1) {
                b.append('.');
            }
        }
        return b.toString();
    }

    public static int[] versionToSegments(String versionString) {
        return versionToSegments(versionString, 0, 0);
    }

    public static int compareVersions(String versionA, String versionB) {
        final int[] componentsA = versionToSegments(versionA), componentsB = versionToSegments(versionB);
        final int max = min(componentsA.length, componentsB.length);
        int res;
        for (int i = 0; i < max; i ++) {
            res = signum(componentsA[i] - componentsB[i]);
            if (res != 0) {
                return res;
            }
        }
        // reverse so that shorter versions sort above longer versions
        return signum(componentsB.length - componentsA.length);
    }

    public static final Comparator<String> VERSION_COMPARATOR = new Comparator<String>() {
        public int compare(final String o1, final String o2) {
            return compareVersions(o1, o2);
        }
    };
}
