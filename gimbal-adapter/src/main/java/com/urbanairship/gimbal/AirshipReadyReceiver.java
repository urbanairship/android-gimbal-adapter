/*
 * Copyright 2016 Urban Airship and Contributors
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

        GimbalAdapter.shared(context).startWithPermissionPrompt("", new GimbalAdapter.PermissionResultCallback() {
            @Override
            public void onResult(boolean enabled) {
                if (enabled) {
                    // permission granted and adapter started
                }
            }
        });
    }
}