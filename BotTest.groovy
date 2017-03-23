// @phsft-bot build just on mac1011/gcc49 ubuntu14/native with flags -Dfoo=don -Dbar=foobar

// Assert default build matrix is discarded
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/gcc49",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/gcc49"])

matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1012"]])




println "returned: "
println r


static void assertMatrixConfiguration(actualMatrix, expected) {
    if (!expected.isEmpty() || !actualMatrix.isEmpty()) {
        expected.each {
            assert(configurationExists(actualMatrix["matrix"], it))
        }
    }
}

static boolean configurationExists(actualConfiguration, expectedConfig) {
    boolean found = false
    actualConfiguration.each {
        if (it.LABEL == expectedConfig["LABEL"]
                && it.COMPILER == expectedConfig["COMPILER"]
                && it.BUILDTYPE == expectedConfig["BUILDTYPE"]) {
            found = true
        }
    }

    return found
}

static void assertEnvVariable(Map actual, Map expectedValues) {
    expectedValues.each { k, v -> 
        assert(actual[k] == v)
    }
}

static Map executeEnvLogic(Map params) {
    Binding binding = new Binding()
    params.each { k, v -> binding.setVariable(k, v) }
    GroovyShell shell = new GroovyShell(binding)

    // Mimic the behavior of ignoring overwritten variables, but keep new ones
    Map returnedMap = (Map) shell.evaluate(new File("EnvLogic.groovy"))
    returnedMap.putAll(params)

    return returnedMap
}

static Map executeMatrix(Map environmentVariables) {
    def combinations = [[BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"slc6"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"lcgapp-centos7-x86-64-28"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"ubuntu14"],
                        [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"mac1011"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"lcgapp-centos7-x86-64-28"],
                        [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"ubuntu14"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"ubuntu14"],
                        [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"centos7"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"slc6"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"mac1011"],
                        [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"slc6"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"centos7"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"mac1011"],
                        [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"centos7"],
                        [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"lcgapp-centos7-x86-64-28"]]

    Binding binding = new Binding()
    binding.setVariable("combinations", combinations)
    binding.setVariable("env", environmentVariables)
    binding.setVariable("result", [:])

    GroovyShell shell = new GroovyShell(binding)
    // Add the Combination mock to the shell
    shell.evaluate(new File("Combination.groovy"))
    return (Map) shell.evaluate(new File("MatrixFilter.groovy"))
}