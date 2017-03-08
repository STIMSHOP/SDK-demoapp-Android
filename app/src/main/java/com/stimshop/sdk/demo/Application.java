package com.stimshop.sdk.demo;

import com.stimshop.sdk.common.StimShop;

/**
 * Custom application class
 * <p/>
 * Created by vprat on 02/07/2015.
 */
public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        StimShop.create(this)
                .enableDebug(true)
// You can also specify the configuration from the code directly if you prefer
//                .withConfiguration(
//                        new JavaConfiguration.Builder()
//                                .apiKey("1234...")
//                                .channels(new int[]{2, 3})
//                                .requestedDetectors(new Detector.Type[]{
//                                        Detector.Type.AUDIO,
//                                        Detector.Type.BLUETOOTH_LE})
//                                .build())
                .start();
    }
}
