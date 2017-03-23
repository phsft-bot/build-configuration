returnValue = execute(["ghprbCommentBody": "@phsft-bot build just on mac1011/gcc49 ubuntu14/native with flags -Dfoo=don -Dbar=foobar",
                                 "_ExtraCMakeOptions": ""])

assertEnvVariable(returnValue, ["addDefaultMatrix": "false"])
println returnValue


def assertEnvVariable(Map actual, Map expectedValues) {
    expectedValues.each { k, v -> 
        assert(actual[k] == v)
    }
}

def execute(Map params) {
    Binding binding = new Binding()
    params.each { k, v -> binding.setVariable(k, v) }
    GroovyShell shell = new GroovyShell(binding)
    return shell.evaluate(new File("EnvLogic.groovy"))
}