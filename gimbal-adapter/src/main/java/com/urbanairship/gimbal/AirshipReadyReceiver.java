/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.urbanairship.gimbal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver for Airship Ready events.
 */
public class AirshipReadyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GimbalAdapter.shared(context).restore();
    }
}