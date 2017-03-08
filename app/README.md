Demo application for StimShop SDK
=================================

This application is meant to be a starting point for people who want to develop their own
application based on the StimShop SDK.

Here is a list of the important files you will have to study and/or modify:

 - `src/main/res/values/stimshop.xml`:

    - This is the configuration file, you will at least need to replace the fake api key with the
      one you have received.

 - `build.gradle`:
    - You have to uncomment the last dependency (stimshop-sdk-commons-release)
    - You have to remove the last part of the script
    - You should change the values in the android block to match the API level, version code, etc.

 - `src/main/AndroidManifest.xml`:
    - Make sure you change the package name to the one of your app



