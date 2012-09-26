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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ImmutableSortedSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final Comparator<? super E> comparator;
    private final E[] values;
    private final int offs, len;

    public ImmutableSortedSet(final Comparator<? super E> comparator, final E... values) {
        final E[] clone = values.clone();
        this.comparator = comparator;
        Arrays.sort(clone, comparator);
        this.values = clone;
        offs = 0;
        len = clone.length;
    }

    private ImmutableSortedSet(final Comparator<? super E> comparator, final E[] values, final int offs, final int len) {
        this.comparator = comparator;
        this.values = values;
        this.offs = offs;
        this.len = len;
    }

    public ImmutableSortedSet(final E... values) {
        this(null, values);
    }

    public Iterator<E> iterator() {
        return null;
    }

    public int size() {
        return len;
    }

    private int inclusiveIndexOf(E e) {
        final int idx = indexOf(e);
        return idx > 0 ? idx : -(idx + 1);
    }

    private int exclusiveIndexOf(E e) {
        final int idx = indexOf(e);
        return idx > 0 ? idx + 1 : -(idx + 2);
    }

    private int indexOf(E e) {
        return Arrays.binarySearch(values, offs, len, e, comparator);
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    public NavigableSet<E> subSet(final E fromElement, final E toElement) {
        final int start = inclusiveIndexOf(fromElement);
        final int end = exclusiveIndexOf(toElement);
        final int len = end - start;
        return new ImmutableSortedSet<E>(comparator, values, start, len);
    }

    public NavigableSet<E> headSet(final E toElement) {
        final int end = exclusiveIndexOf(toElement);
        return new ImmutableSortedSet<E>(comparator, values, offs, end - offs);
    }

    public NavigableSet<E> tailSet(final E fromElement) {
        final int idx = inclusiveIndexOf(fromElement);
        return new ImmutableSortedSet<E>(comparator, values, idx, len - (idx - offs));
    }

    public E first() {
        return len > 0 ? values[offs] : null;
    }

    public E last() {
        return len > 0 ? values[offs + len - 1] : null;
    }

    public E lower(final E e) {
        return len > 0 ? values[exclusiveIndexOf(e)] : null;
    }

    public E floor(final E e) {
        return len > 0 ? values[inclusiveIndexOf(e)] : null;
    }

    public E ceiling(final E e) {
        return null;
    }

    public E higher(final E e) {
        return null;
    }

    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> descendingSet() {
        throw new UnsupportedOperationException();
    }

    public Iterator<E> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        return null;
    }

    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return null;
    }

    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return null;
    }
}
