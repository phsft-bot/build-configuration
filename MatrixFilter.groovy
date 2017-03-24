import hudson.matrix.Combination

availableCombinations = []

combinations.each{
    availableCombinations.add(formatCombination(it.COMPILER, it.LABEL))
}

StringBuilder commentResponse = new StringBuilder()
StringBuilder unrecognizedPlatforms = new StringBuilder()
commentResponse.append("Starting build on ")

if (env.containsKey("matrixConfig") && env.matrixConfig.size() > 0) {
    def patterns = env.matrixConfig.replace("," ,"").split(" ")

    for (String unparsedPattern : patterns) {
        def patternArgs = unparsedPattern.split("/")
        addMatrixConfiguration(patternArgs[1], patternArgs[0], commentResponse)

        println "Received label " + patternArgs[0] + " with compiler " + patternArgs[1]

        if (!recognizedPlatform(patternArgs[1], patternArgs[0])) {
            unrecognizedPlatforms.append("`" + patternArgs[1] + "`/`" + patternArgs[0] + "`, ")
        }
    }
} else {
    println "Warning: matrixConfig not set"
}

if (unrecognizedPlatforms.length() > 0) {
    unrecognizedPlatforms.replace(unrecognizedPlatforms.length() - 2, unrecognizedPlatforms.length(), " ")
    postComment("Didn't recognize " + unrecognizedPlatforms.toString().trim() + " aborting build.")
    throw new Exception("Unrecognized compiler(s)/platform(s): " + unrecognizedPlatforms.toString())
}

if (!env.containsKey("addDefaultMatrix") || String.valueOf(env.addDefaultMatrix).equals("true")) {
    // Default matrix configuration for pull requests:
    addMatrixConfiguration("gcc49", "centos7", commentResponse)
    addMatrixConfiguration("native", "mac1011", commentResponse)
    addMatrixConfiguration("gcc49", "slc6", commentResponse)
    addMatrixConfiguration("gcc62", "slc6", commentResponse)
    addMatrixConfiguration("native", "ubuntu14", commentResponse)
} else {
    println "No default config set"
}

// Remove last "," after platforms listing
commentResponse.replace(commentResponse.length() - 2, commentResponse.length(), " ")

if (env.ExtraCMakeOptions != null && env.ExtraCMakeOptions.size() > 0) {
    commentResponse.append("and CMake flags `" + env.ExtraCMakeOptions + "`")
}

static String formatCombination(String compiler, String platform) {
    return "BUILDTYPE=Debug,COMPILER=" + compiler + ",LABEL=" + platform
}

boolean recognizedPlatform(String compiler, String platform) {
    return availableCombinations.contains(formatCombination(compiler, platform))
}

void addMatrixConfiguration(String compiler, String platform, StringBuilder commentBuilder) {
    if (!recognizedPlatform(compiler, platform)) {
        println "WARNING: " + platform + "/" + compiler + " is not recognized, skipping"
    } else {
        commentBuilder.append("`" + platform + "`/`" + compiler + "`, ")
        addCombinationFromString(formatCombination(compiler, platform))
    }
}

void addCombinationFromString(String combinationString) {
    println "Adding combination " + combinationString
    def combination = Combination.fromString(combinationString)
    result["matrix"] = result["matrix"] ?: []
    result["matrix"] << combination
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
        // Class path does not want to work, workaround to get around this hence toString()
        if (v.getClass().toString().equals("class org.jenkinsci.plugins.ghprb.GhprbTrigger")) {
            ghprbTrigger = v
        }
    }


    ghprbTrigger.getRepository().addComment(Integer.valueOf(env.ghprbPullId), comment)
}

postComment(commentResponse.toString())

result