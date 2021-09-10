fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android build
```
fastlane android build
```
Build using the given environment (default: Staging) and build type (default: Debug).
### android staging_release
```
fastlane android staging_release
```
Build Staging Release
### android prod_debug
```
fastlane android prod_debug
```
Build Prod Debug
### android prod_release
```
fastlane android prod_release
```
Build Prod Release
### android test
```
fastlane android test
```
Run tests. Optional flags: environment (Staging), build_type (Debug), module(app), test_name (runs all by default). Environment and build_type are app module-only.
### android upload_to_internal_track
```
fastlane android upload_to_internal_track
```
Submit a release build to the Play Store internal test track.
### android credentials
```
fastlane android credentials
```
Get the configuration files from the Android credentials repository.
### android ci_run_tests
```
fastlane android ci_run_tests
```
Bundle of build, perform checks and run tests on CI.
### android ci_test_app
```
fastlane android ci_test_app
```
Tests to run on CI app
### android ci_test_balance
```
fastlane android ci_test_balance
```
Tests to run on CI balance
### android ci_test_common
```
fastlane android ci_test_common
```
Tests to run on CI common
### android ci_test_blockchainApi
```
fastlane android ci_test_blockchainApi
```
Tests to run on CI blockchainApi
### android ci_test_core
```
fastlane android ci_test_core
```
Tests to run on CI core
```
### android ci_test_notifications
```
fastlane android ci_test_notifications
```
Tests to run on CI notifications
### android ci_test_sunriver
```
fastlane android ci_test_sunriver
```
Tests to run on CI sunriver
### android ci_test_testutils
```
fastlane android ci_test_testutils
```
Tests to run on CI testutils
### android ci_test_testutils_android
```
fastlane android ci_test_testutils_android
```
Tests to run on CI testutils-android
### android ci_test_wallet
```
fastlane android ci_test_wallet
```
Tests to run on CI wallet
### android ci_credentials
```
fastlane android ci_credentials
```
Get the configuration files from the Android credentials repository on CI.
### android ci_credentials_cleanup
```
fastlane android ci_credentials_cleanup
```
Cleanup the credentials repository
### android ci_upload_to_appcenter
```
fastlane android ci_upload_to_appcenter
```
Upload to AppCenter.
### android ci_export_build
```
fastlane android ci_export_build
```
Export the build path to environment variables for upload. Optional flags: export_bundle (APK is default), do_sign (False is default).
### android ci_build
```
fastlane android ci_build
```
Build to run on CI. Optional flags: copy_credentials, build_bundle (APK is default), export_build(False is default), do_sign (False is default).
### android ci_lint
```
fastlane android ci_lint
```
Checks to run on CI

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
