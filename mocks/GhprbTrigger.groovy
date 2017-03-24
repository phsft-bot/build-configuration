/**
 * Mock class for GhprbTrigger
 */

package org.jenkinsci.plugins.ghprb

class GhprbTrigger {
    int expectedId
    String expectedComment
    boolean wasTriggered

    GhprbTrigger(int expectedId, String expectedComment) {
        this.expectedId = expectedId
        this.expectedComment = expectedComment
    }

    GhprbTrigger getRepository() {
        return this
    }

    void addComment(int prId, String comment) {
        assert(prId == expectedId)
        assert(comment == expectedComment)
        wasTriggered = true
    }

    static void main(String[] args) { }
}
