/**
 * Mock class for GhprbTrigger
 */

package org.jenkinsci.plugins.ghprb

class GhprbTrigger {
    int prId
    String comment
    boolean triggered

    def getRepository() {
        return this
    }

    void addComment(int prId, String comment) {
        // Should always just post one comment.
        assert(!triggered)

        this.prId = prId
        this.comment = comment
        this.triggered = true
    }

    static void main(String[] args) { }
}

static def mock() {
    return new org.jenkinsci.plugins.ghprb.GhprbTrigger()
}
