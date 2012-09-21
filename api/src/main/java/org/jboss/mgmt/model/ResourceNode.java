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

package org.jboss.mgmt.model;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ResourceNode<R> {
    private volatile int state;
    private volatile R current;
    private volatile Object owningTxn;
    private volatile Thread waiter;

    private static final AtomicIntegerFieldUpdater<ResourceNode> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(ResourceNode.class, "state");
    private static final AtomicReferenceFieldUpdater<ResourceNode, Object> currentUpdater = AtomicReferenceFieldUpdater.newUpdater(ResourceNode.class, Object.class, "current");
    private static final AtomicReferenceFieldUpdater<ResourceNode, Object> owningTxnUpdater = AtomicReferenceFieldUpdater.newUpdater(ResourceNode.class, Object.class, "owningTxn");
    private static final AtomicReferenceFieldUpdater<ResourceNode, Thread> waiterUpdater = AtomicReferenceFieldUpdater.newUpdater(ResourceNode.class, Thread.class, "waiter");

    private static final int WRITE_LOCK = (1 << 31);
    private static final int INTENT_WRITE_LOCK = (1 << 30);
    private static final int READERS = INTENT_WRITE_LOCK - 1;

    private static Object currentTransaction() {
        return null; // todo once msc-2 stabilizes...
    }

    protected R getCurrent(boolean writeLock) {
        if (writeLock) {
            lockWrite(currentTransaction());
        } else {
            lockRead(currentTransaction());
        }
        // if (transaction.get(this) != null) { return it; }
        return current;
    }

    protected void modify(R newValue) {
        lockWrite(currentTransaction());
        // transaction.put(this, newValue);
    }

    private void lockRead(final Object txn) {
        // increment count or block with txn
    }

    private void lockWrite(final Object txn) {
        // set write flag or block with txn
    }
}
