import java.util.regex.Pattern

def comment = ghprbCommentBody.trim()

final COMMENT_REGEX = "build ((?<overrideMatrix>just|also) on (?<matrix>(\\S*\\/\\S*,?\\s?)*))?(with flags (?<flags>.*))?"
def matcher = Pattern.compile(COMMENT_REGEX).matcher(comment)

def environment = [matrixConfig: "", ExtraCMakeOptions: "", addDefaultMatrix: "true"]

if (matcher.find()) {
    println "Comment recognized as a parseable command"

    addDefaultMatrix = !matcher.group("overrideMatrix").equals("just")
    def compilerFlags = matcher.group("flags")
    def unparsedMatrixConfig = matcher.group("matrix")

    if (unparsedMatrixConfig != null) {
        environment.matrixConfig = unparsedMatrixConfig.trim()
    }

    if (compilerFlags != null) {
        def cmakeFlagsMap = [:]
        appendFlagsToMap(_ExtraCMakeOptions, cmakeFlagsMap)
        appendFlagsToMap(compilerFlags, cmakeFlagsMap)

        completeOptions = cmakeFlagsMap.collect { /$it.key=$it.value/ } join " "
        
        environment.ExtraCMakeOptions = completeOptions
        println "ExtraCMakeOptions set to " + completeOptions
        println "Ref: " + environment["ExtraCMakeOptions"]
    } else {
        environment.ExtraCMakeOptions = _ExtraCMakeOptions
    }


    environment.addDefaultMatrix = String.valueOf(addDefaultMatrix)


    println "Override matrix: " + addDefaultMatrix
    println "Flags: " + compilerFlags
    println "Matrix config: " + unparsedMatrixConfig
} else {
    println "Unrecognizable comment: " + comment
    environment.addDefaultMatrix = true
}


static void appendFlagsToMap(flags, map) {
    def parsedCompilerFlags = flags.split(" ")
    for (String unparsedFlag : parsedCompilerFlags) {
        if (unparsedFlag.contains("=")) {
            def flag = unparsedFlag.split("=")

            if (map.containsKey(flag[0])) {
                map[flag[0]] = flag[1]
            } else {
                map.put(flag[0], flag[1])
            }
        }
    }
}

return environment
