## Snapshots
Currently (as of Dec 2023), Desktop and Android snapshots have been automated via GithubActions. 

You can see the workflows here.
[![Create Desktop/Android Snapshot](https://github.com/Card-Forge/forge/actions/workflows/snapshot-both-pc-android.yml/badge.svg)](https://github.com/Card-Forge/forge/actions/workflows/snapshot-both-pc-android.yml)
These run daily at around 2pm ET time for people looking for them. The upload scripts should take care of removing the previous files uploaded on the last run. If necessary, you can run these scripts manually. Occasionally these builds fail to upload due to unclear reasons. Retrying the snapshot usually seems to fix the issue. (As of 2/23/2025, the tag is tagging an old SHA. We'll try to resolve that, but know that the snapshot file time represents which code is utilized.)

The latest daily snapshots can be found [here](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots)

## Releases
Currently (as of Dec 2023), Desktop releases run an automated script with a few manual pieces. 

Currently (as of Dec 4, 2023), Android releases are not run. Although, hopefully with the Android snapshot now automated we'll be able to get back to getting a full release for Android as well. 

### Desktop Release Process
It would be useful for other people to be comfortable with the release process. Currently, GitHub has the secrets required to add a snapshot or full release, so it's mostly just running through the steps below to kick off and validate a release. 

1. Run Build
* Verify test suite is passing on the repo
[![Test build](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml/badge.svg?branch=master)](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml)
* Create a new branch from the default branch
* Run the https://github.com/Card-Forge/forge/actions/workflows/maven-publish.yml workflow on the new branch
2. Verify
* After it completes, download the finished package. Extract it and do a quick verification that it works.
3. For snapshot updates, a few files currently need manual updates. Check out this PR for an example: https://github.com/Card-Forge/forge/pull/4293/files
(Hopefully in the next few iterations, we'll have this automated during the release)
* forge-gui-desktop/pom.xml 
* fromRef
* forge-gui-android/AndroidManifest.xml
* android::versionCode
* android::versionName
* forge-gui-android/pom.xml
* alpha-version
* forge-gui-mobile/src/forge/Forge.java
* CURRENT_VERSION
4. Merge
* Create a PR from your branch and get it merged as quickly as you can (ideally before other PRs are merged). 
* Create a new release from https://github.com/Card-Forge/forge/releases
* Upload the package and its sha to the create new release page
5. Marketing
* Advertise in the #announcements channel in the Discord

### Updating an existing installation with a local build

Often, to test your code, you may want to use an existing installation and overwrite only the code files.

The following steps will help perform an incremental update to your existing installation.

1. Run `mvn clean install -DskipTests` from the main project directory to build the artifacts.
2. Copy `forge-gui-desktop/target/forge-gui-desktop-${VERSION}-jar-with-dependencies.jar` to the root installation directory.
3. Copy `forge-gui-mobile-dev/target/forge-gui-mobile-dev-*-jar-with-dependencies.jar` to the root installation directory.
4. Make sure the scripts you use to launch Forge, located in the root instalation directory, are referencing the copied files.

