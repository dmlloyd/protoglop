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

/**
 * Base class for all mutable node classes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractMutableNode<N extends Node> implements Node {
    private volatile N current;
    private final AbstractMutableNode<?> parent;

    protected AbstractMutableNode(final AbstractMutableNode<?> parent, final N initialValue) {
        this.parent = parent;
        current = initialValue;
    }

    public String getName() {
        return current.getName();
    }

    public Node getParent() {
        return parent;
    }

    protected final N getCurrent() {
        return current;
    }

    protected void modify(N newValue) {
        current = newValue;
    }

    protected final Object writeReplace() {
        return getCurrent();
    }
}
