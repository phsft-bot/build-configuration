# Prevent Jenkins builds
Mention `[skip-ci]` in the title or body of a pull request to prevent Jenkins from automatically testing it. Useful for instance for documentation changes.

# Build Configuration Integration
This configuration allows administrators of @phsft-bot bot to specify which platform and compiler flags to build pullrequests on.

# Grammar (BNF notation)

```
command   ::= "@phsft-bot build" [platforms] [flags]
platforms ::= ("also"|"just") "on" platform { [","] platform } 
platform  ::= label/spec
label     ::= "ROOT-centos7"|"mac1014"|"mac10beta"|"ROOT-ubuntu16"|"ROOT-fedora30"|"windows10"|...
spec      ::= "default"|"python3"|"rtcxxmod"|"noimt"|"cxx14"|"cxx17"
flags     ::= "with flags" { flag }
```

Note: The phrase can be ended by endline. What comes on the other lines will not be interpreted by the bot. Specified flags in the command will overwrite conflicting environment variables that is already set for the job.
## Examples:
##### @phsft-bot build
Starts build on default build configuration.

##### @phsft-bot build with flags -DCTEST_TEST_EXCLUDE_NONE=On
Starts build on default build configuration and run all the tests.

##### @phsft-bot build just on ROOT-centos7/default, ROOT-ubuntu14/noimt with flags -Dtcmalloc=ON
Discards default build matrix configuration in Jenkins and builds only on Centos7/gcc49 and Ubuntu 14/native with the CMake flags `-Dtcmalloc=ON`.

##### @phsft-bot build also on ROOT-centos7/default, ROOT-ubuntu14/noimt with flags -Dtcmalloc=ON
Same as previous example but does not discard default matrix configuration.

##### @phsft-bot build also on ROOT-centos7/default, ROOT-ubuntu14/noimt
Same as previous example without any additional CMake flags.


# Setup
To set this up, enter the Jenkins pull request job configuration and copy the contents of `EnvLogic.groovy` to "Evaluated Groovy script" under "Prepare an environment for the run" and `MatrixFilter.groovy` to the "Groovy Script" field under "Execution strategy" (make sure Groovy Script Matrix Executor Strategy) is selected. Under "This project is parameterized", make sure the `ExtraCMakeOptions` parameter is prefixed with `_` and the "Default Filter" for "Matrix Combinations Parameter" is empty. Default matrix configuration is now set under the contents of MatrixFilter.groovy.

# Test
To run the tests on the bot, run:

    $ groovy BotTest.groovy
