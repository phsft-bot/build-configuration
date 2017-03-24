import hudson.matrix.Combination

availableCombinations = []

combinations.each{
    availableCombinations.add(formatCombination(it.COMPILER, it.LABEL))
}

if (env.containsKey("matrixConfig") && env.matrixConfig.size() > 0) {
    def patterns = env.matrixConfig.replace("," ,"").split(" ")

    for (String unparsedPattern : patterns) {
        def patternArgs = unparsedPattern.split("/")
        addMatrixConfiguration(patternArgs[1], patternArgs[0])

        println "Received label " + patternArgs[0] + " with compiler " + patternArgs[1]
    }
} else {
    println "Warning: matrixConfig not set"
}

if (!env.containsKey("addDefaultMatrix") || String.valueOf(env.addDefaultMatrix).equals("true")) {
    // Default matrix configuration for pull requests:
    addMatrixConfiguration("gcc49", "centos7")
    addMatrixConfiguration("native", "mac1011")
    addMatrixConfiguration("gcc49", "slc6")
    addMatrixConfiguration("gcc62", "slc6")
    addMatrixConfiguration("native", "ubuntu14")
} else {
    println "No default config set"
}

static String formatCombination(String compiler, String platform) {
    return "BUILDTYPE=Debug,COMPILER=" + compiler + ",LABEL=" + platform
}

void addMatrixConfiguration(String compiler, String platform) {
    addCombinationFromString(formatCombination(compiler, platform))
}

void addCombinationFromString(String combinationString) {
    if (!availableCombinations.contains(combinationString)) {
        println "WARNING: Combination " + combinationString + " is not recognized, skipping"
    } else {
        println "Adding combination " + combinationString
        def combination = Combination.fromString(combinationString)
        result["matrix"] = result["matrix"] ?: []
        result["matrix"] << combination
    }
}

void postComment(String comment) {
    if (!binding.variables.containsKey("jenkins") || jenkins == null) {
        println "Warning: jenkins is null, ignoring comment [" + comment + "]"
        return
    }

    // Ugly hack to get ahold the GhprbTriggerMock, otherwise jenkins.getJob().getTrigger(classname)
    // would work, but getTrigger does not exist despite the fact that it is documented.
    def ghprbTrigger = null
    jenkins.getJob(env.JOB_NAME).getTriggers().each { k, v ->
        println v.getClass().toString()
        // Class path does not want to work, workaround to get around this hence toString()
        if (v.getClass().toString().equals("class org.jenkinsci.plugins.ghprb.GhprbTrigger")) {
            ghprbTrigger = v
        }
    }


    ghprbTrigger.getRepository().addComment(Integer.valueOf(env.ghprbPullId), comment)
}

postComment("test")

result