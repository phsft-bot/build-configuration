/**
 * Mock class for hudson.matrix.Combination
 */

package hudson.matrix

class Combination {
    String COMPILER
    String LABEL
    String BUILDTYPE

    static Combination fromString(String input) {
        Map variables = [:]
        input.split(",").each {
            def args = it.split('=')
            variables.put(args[0], args[1])
        }

        Combination combination = new Combination()
        combination.COMPILER = variables["COMPILER"]
        combination.LABEL = variables["LABEL"]
        combination.BUILDTYPE = variables["BUILDTYPE"]

        return combination
    }

    static void main(String[] args) { }

    String toString() {
        return "BUILDTYPE=" + BUILDTYPE + ",COMPILER=" + COMPILER + ",LABEL=" + LABEL
    }
}