/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.urbanairship.gimbal;

import android.content.Context;
import android.support.annotation.NonNull;

import com.urbanairship.AirshipReceiver;


/**
 * Urban Airship event listener.
 */
public class GimbalAirshipReceiver extends AirshipReceiver {

    @Override
    protected void onChannelCreated(@NonNull Context context, @NonNull String channelId) {
        super.onChannelCreated(context, channelId);
        GimbalAdapter.shared(context).updateDeviceAttributes();
    }
}
