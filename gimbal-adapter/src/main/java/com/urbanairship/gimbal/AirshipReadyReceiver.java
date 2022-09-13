/*
 * Copyright 2018 Urban Airship and Contributors
 */

package com.urbanairship.gimbal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

import com.urbanairship.UAirship;
import com.urbanairship.channel.AirshipChannelListener;

/**
 * Broadcast receiver for Airship Ready events.
 */
public class AirshipReadyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (UAirship.isFlying() || UAirship.isTakingOff()) {
            GimbalAdapter.shared(context).restore();

            UAirship.shared().getChannel().addChannelListener(new AirshipChannelListener() {
                @Override
                public void onChannelCreated(@NonNull String channelId) {
                    GimbalAdapter.shared(context).onAirshipChannelCreated();
                }

                @Override
                public void onChannelUpdated(@NonNull String channelId) {
                }
            });
        }
    }
}