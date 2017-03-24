class Config {
    static ALL_COMBINATIONS =     [[BUILDTYPE:"Debug", COMPILER:"native", LABEL:"mac1011"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"mac1011"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"centos7"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"clang_gcc52", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"clang_gcc62", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"ubuntu14"]]

    static DEFAULT_COMBINATIONS = [[BUILDTYPE:"Debug", COMPILER:"native", LABEL:"mac1011"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"centos7"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc49", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"gcc62", LABEL:"slc6"],
                                   [BUILDTYPE:"Debug", COMPILER:"native", LABEL:"ubuntu14"]]
}

static void assertMatrixConfiguration(actualMatrix, expected) {
    if (!expected.isEmpty() || !actualMatrix.isEmpty()) {
        expected.each {
            assert(configurationExists(actualMatrix["matrix"], it))
        }

        actualMatrix["matrix"].each {
            assert(configurationExists(expected, it))
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
        assert(String.valueOf(actual[k]) == String.valueOf(v))
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

static Map executeMatrix(Map environmentVariables, Jenkins jenkins = null) {
    if (jenkins != null) {
        environmentVariables.put("JOB_NAME", jenkins.jobName)
    }

    Binding binding = new Binding()
    binding.setVariable("combinations", Config.ALL_COMBINATIONS)
    binding.setVariable("env", environmentVariables)
    binding.setVariable("result", [:])
    binding.setVariable("jenkins", jenkins)

    GroovyShell shell = new GroovyShell(binding)
    // Add the Combination mock to the shell
    shell.evaluate(new File("mocks/Combination.groovy"))
    return (Map) shell.evaluate(new File("MatrixFilter.groovy"))
}

class Jenkins {
    String jobName
    Object trigger

    Jenkins(String jobName, Object trigger) {
        this.jobName = jobName
        this.trigger = trigger

        assert(trigger.getClass().toString().equals("class org.jenkinsci.plugins.ghprb.GhprbTrigger"))
    }

    Jenkins getJob(String jobName) {
        assert(jobName == this.jobName)
        return this
    }

    Map getTriggers() {
        return [k: trigger]
    }
}

GroovyShell shell = new GroovyShell()
def ghprbTriggerMockFactory = shell.parse(new File("mocks/GhprbTriggerMock.groovy"))
// Assert default build matrix is discarded
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/gcc49",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/gcc49"])

matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1011"]])

// Bot should run default build with no recognizable command
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build!",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [matrixConfig: "", ExtraCMakeOptions: "", addDefaultMatrix: "true"])

matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, Config.DEFAULT_COMBINATIONS)

// Default build matrix is not discarded
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build also on mac1011/gcc49",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "true", matrixConfig: "mac1011/gcc49"])

matrix = executeMatrix(returnValue)
expectedMatrix = Config.DEFAULT_COMBINATIONS.collect()
expectedMatrix.add([BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1011"])
assertMatrixConfiguration(matrix, expectedMatrix)

// Just cmake options are read
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build with flags -Dfoo=bar",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "true", matrixConfig: "", ExtraCMakeOptions: "-Dfoo=bar"])

// Cmake flags are overwritten
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build with flags -Dfoo=bar",
                               _ExtraCMakeOptions: "-Dfoo=don"])
assertEnvVariable(returnValue, [addDefaultMatrix: "true", matrixConfig: "", ExtraCMakeOptions: "-Dfoo=bar"])

// Multiple platforms are added
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/gcc49 ubuntu14/native",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/gcc49 ubuntu14/native"])
matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1011"],
                                   [BUILDTYPE: "Debug", COMPILER: "native", LABEL: "ubuntu14"]])

// Multiple platforms are added separated by comma
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/gcc49, ubuntu14/native",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/gcc49, ubuntu14/native"])
matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1011"],
                                   [BUILDTYPE: "Debug", COMPILER: "native", LABEL: "ubuntu14"]])

// Ignore unsupported platforms
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/blaah, blaah/native",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/blaah, blaah/native"])
matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [])

// Newlines are not part of the command with flags
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build with flags -Dfoo=bar\nhello this is dog",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "true", matrixConfig: "", ExtraCMakeOptions: "-Dfoo=bar"])

// Period are not part of the command with platforms
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on mac1011/gcc49.",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "mac1011/gcc49"])
matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "gcc49", LABEL: "mac1011"]])

// Underscores are recognized
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on slc6/clang_gcc52",
                               _ExtraCMakeOptions: ""])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "slc6/clang_gcc52"])
matrix = executeMatrix(returnValue)
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "clang_gcc52", LABEL: "slc6"]])

// Right comment is posted
triggerMock = ghprbTriggerMockFactory.mock()
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build just on slc6/clang_gcc52",
                               _ExtraCMakeOptions: "", ghprbPullId: 3])
assertEnvVariable(returnValue, [addDefaultMatrix: "false", matrixConfig: "slc6/clang_gcc52"])
matrix = executeMatrix(returnValue, new Jenkins("foo", triggerMock))
assertMatrixConfiguration(matrix, [[BUILDTYPE: "Debug", COMPILER: "clang_gcc52", LABEL: "slc6"]])
assert(triggerMock.prId == 3)
assert(triggerMock.comment.size() > 0)
assert(triggerMock.triggered)

// Assert cmake flags are posted in comments
triggerMock = ghprbTriggerMockFactory.mock()
returnValue = executeEnvLogic([ghprbCommentBody  : "@phsft-bot build with flags -Dfoo=bar",
                               _ExtraCMakeOptions: "", ghprbPullId: 3])
assertEnvVariable(returnValue, [addDefaultMatrix: "true", matrixConfig: "", ExtraCMakeOptions: "-Dfoo=bar"])
matrix = executeMatrix(returnValue, new Jenkins("foo", triggerMock))
assertMatrixConfiguration(matrix, Config.DEFAULT_COMBINATIONS)

assert(triggerMock.triggered)
assert(triggerMock.comment.contains("-Dfoo=bar"))

println "\nAll tests passing"