/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.core.management;

import org.jboss.msc.txn.Listener;
import org.jboss.msc.txn.TransactionalContext;

/**
 * A managed system.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ManagedSystem {

    // Immutable state

    private static final ThreadLocal<TransactionalContext> SYS_CONTEXT = new ThreadLocal<>();

    /**
     * The path element key which defines the model type of this managed system.
     */
    private final String rootPathKey;

    /**
     * The root resource of this managed system.
     */
    private final AbstractMutableResource<?> rootResource;

    // Mutable state

    /**
     * The current run level of this system.
     */
    private RunLevel runLevel;

    ManagedSystem(final String rootPathKey, final AbstractMutableResource<?> rootResource) {
        this.rootPathKey = rootPathKey;
        this.rootResource = rootResource;
    }

    public RunLevel getRunLevel() {
        return runLevel;
    }

    /**
     * Get a resource at an address.
     *
     * @param address
     * @return
     */
    public Resource getResource(PathAddress address) {
        return null;
    }

    public static TransactionalContext setTransactionalContext(TransactionalContext context) {
        try {
            return SYS_CONTEXT.get();
        } finally {
            SYS_CONTEXT.set(context);
        }
    }

    public static TransactionalContext getTransactionalContext() {
        return SYS_CONTEXT.get();
    }

    public boolean changeRunLevel(RunLevel oldLevel, RunLevel newLevel, Listener<ManagedSystem> completionListener) {
        final TransactionalContext context = SYS_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("No transaction");
        }

    }
}
