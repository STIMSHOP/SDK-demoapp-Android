Demo application for StimShop SDK
=================================

This application is meant to be a starting point for people who want to develop their own
application based on the StimShop SDK.

It also shows how to listen to the SDK's permission request at runtime.

Here is a list of the important files you will have to study and/or modify after importing the SDK's AAR file to the project in `app/libs`:

 - `app/src/main/res/values/stimshop.xml`:
    - This is the configuration file, you will at least need to replace the fake api key with the
      one you have received.

 - `app/build.gradle`:
    - You should not remove any of the SDK's own dependencies (ie: retrofit, jtransforms etc.), unless you know what you are doing
    - You should change the values in the android block to match the API level, version code, etc.

 - `app/src/main/AndroidManifest.xml`:
    - Make sure you change the package name to the one of your app
