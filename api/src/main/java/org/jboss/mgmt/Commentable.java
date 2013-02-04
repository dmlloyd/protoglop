package org.jboss.mgmt;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Commentable {

    /**
     * Get the stored comment string for this resource.  The comment is persisted before the start
     * of the resource's element.
     *
     * @return the comment text
     */
    String getPreComment();

    /**
     * Get the stored post-element string for this resource.  The comment is persisted before the close
     * of the resource's element.
     *
     * @return the comment text
     */
    String getPostComment();
}
