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

        println "Recognized label " + patternArgs[0] + " with compiler " + patternArgs[1]
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

result