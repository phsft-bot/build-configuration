import java.util.regex.Pattern

def comment = ghprbCommentBody.toLowerCase()

final COMMENT_REGEX = "build ((?<overrideMatrix>just|also) on (?<matrix>((centos7|mac1011|slc6|ubuntu14)\\/(gcc49|gcc62|native),?\\s?)*))?(with flags (?<flags>.*))?"
def matcher = Pattern.compile(COMMENT_REGEX).matcher(comment)

def environment = [:]

if (matcher.find()) {
    println "Comment recognized as a parseable command"

    addDefaultMatrix = !matcher.group("overrideMatrix").equals("just")
    def compilerFlags = matcher.group("flags")
    def unparsedMatrixConfig = matcher.group("matrix")

    if (unparsedMatrixConfig != null) {
        environment.put("matrixConfig", unparsedMatrixConfig.trim())
    }

    if (compilerFlags != null) {
        def cmakeFlagsMap = [:]
        appendFlagsToMap(compilerFlags, cmakeFlagsMap)
        appendFlagsToMap(_ExtraCMakeOptions, cmakeFlagsMap)
       
        completeOptions = cmakeFlagsMap.collect { /$it.key=$it.value/ } join " "
        
        environment.put("ExtraCMakeOptions", completeOptions)
        println "ExtraCMakeOptions set to " + completeOptions
        println "Ref: " + environment["ExtraCMakeOptions"]
    } else {
        environment.put("ExtraCMakeOptions", _ExtraCMakeOptions)
    }


    environment.put("addDefaultMatrix", String.valueOf(addDefaultMatrix))


    println "Override matrix: " + addDefaultMatrix
    println "Flags: " + compilerFlags
    println "Matrix config: " + unparsedMatrixConfig
} else {
    println "Unrecognizable comment: " + comment
}


static void appendFlagsToMap(flags, map) {
    def parsedCompilerFlags = flags.split(" ")
    for (String unparsedFlag : parsedCompilerFlags) {
        if (unparsedFlag.contains("=")) {
            def flag = unparsedFlag.split("=")

            if (map.containsKey(flag[0])) {
                map[flag[0]] = flag[1];
            } else {
                map.put(flag[0], flag[1])
            }
        }
    }
}

return environment
