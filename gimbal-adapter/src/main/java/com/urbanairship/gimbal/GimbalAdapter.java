/*
 * Copyright 2017 Urban Airship and Contributors
 */

package com.urbanairship.gimbal;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.gimbal.android.DeviceAttributesManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.urbanairship.UAirship;
import com.urbanairship.location.RegionEvent;
import com.urbanairship.util.DateUtils;
import com.urbanairship.util.HelperActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * GimbalAdapter interfaces Gimbal SDK functionality with Urban Airship services.
 */
public class GimbalAdapter {
    private static final String PREFERENCE_NAME = "com.urbanairship.gimbal.preferences";
    private static final String API_KEY_PREFERENCE = "com.urbanairship.gimbal.api_key";
    private static final String STARTED_REFERENCE = "com.urbanairship.gimbal.is_started";

    private static final String TAG = "GimbalAdapter";
    private static final String SOURCE = "Gimbal";

    // UA to Gimbal Device Attributes
    private static final String GIMBAL_UA_NAMED_USER_ID = "ua.nameduser.id";
    private static final String GIMBAL_UA_CHANNEL_ID = "ua.channel.id";

    // Gimbal to UA Device Attributes
    private static final String UA_GIMBAL_APPLICATION_INSTANCE_ID = "com.urbanairship.gimbal.aii";

    private final SharedPreferences preferences;
    private static GimbalAdapter instance;
    private final Context context;
    private final List<Listener> listeners = new ArrayList<>();
    private boolean isAdapterStarted = false;
    private RequestPermissionsTask requestPermissionsTask;


    /**
     * Permission result callback.
     */
    public interface PermissionResultCallback {

        /**
         * Called with the permission result.
         *
         * @param enabled {@link true} if the permissions have been granted, otherwise {@code false}.
         */
        void onResult(boolean enabled);
    }

    /**
     * Adapter listener.
     */
    public interface Listener {

        /**
         * Called when a Urban Airship Region enter event is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onRegionEntered(RegionEvent event, Visit visit);

        /**
         * Called when a Urban Airship Region exit event is created from a Gimbal Visit.
         *
         * @param event The Urban Airship event.
         * @param visit The Gimbal visit.
         */
        void onRegionExited(RegionEvent event, Visit visit);
    }

    /**
     * Listener for Gimbal place events. Creates an analytics event
     * corresponding to boundary event type.
     */
    private PlaceEventListener placeEventListener = new PlaceEventListener() {
        @Override
        public void onVisitStart(final Visit visit) {
            Log.i(TAG, "Entered place: " + visit.getPlace().getName() + "Entrance date: " +
                    DateUtils.createIso8601TimeStamp(visit.getArrivalTimeInMillis()));

            UAirship.shared(new UAirship.OnReadyCallback() {
                @Override
                public void onAirshipReady(UAirship airship) {
                    RegionEvent enter = new RegionEvent(visit.getPlace().getIdentifier(), SOURCE, RegionEvent.BOUNDARY_EVENT_ENTER);
                    airship.getAnalytics().addEvent(enter);

                    synchronized (listeners) {
                        for (Listener listener : new ArrayList<>(listeners)) {
                            listener.onRegionEntered(enter, visit);
                        }
                    }
                }
            });
        }

        @Override
        public void onVisitEnd(final Visit visit) {
            Log.i(TAG, "Exited place: " + visit.getPlace().getName() + "Entrance date: " +
                    DateUtils.createIso8601TimeStamp(visit.getArrivalTimeInMillis()) + "Exit date:" +
                    DateUtils.createIso8601TimeStamp(visit.getDepartureTimeInMillis()));

            UAirship.shared(new UAirship.OnReadyCallback() {
                @Override
                public void onAirshipReady(UAirship airship) {
                    RegionEvent exit = new RegionEvent(visit.getPlace().getIdentifier(), SOURCE, RegionEvent.BOUNDARY_EVENT_EXIT);
                    airship.getAnalytics().addEvent(exit);

                    synchronized (listeners) {
                        for (Listener listener : new ArrayList<>(listeners)) {
                            listener.onRegionExited(exit, visit);
                        }
                    }
                }
            });
        }
    };

    /**
     * Hidden to support the singleton pattern.
     *
     * @param context The application context.
     */
    GimbalAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * GimbalAdapter shared instance.
     */
    public synchronized static GimbalAdapter shared(Context context) {
        if (instance == null) {
            instance = new GimbalAdapter(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Adds an adapter listener.
     *
     * @param listener The listener.
     */
    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an adapter listener.
     *
     * @param listener The listener.
     */
    public void removeListner(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Restores the last run state. If previously started it will start listening, otherwise
     * it will stop listening. Should be called when the application starts up.
     */
    public void restore() {
        String gimbalApiKey = preferences.getString(API_KEY_PREFERENCE, null);
        boolean previouslyStarted = preferences.getBoolean(STARTED_REFERENCE, false);
        if (gimbalApiKey != null && previouslyStarted) {
            Log.i(TAG, "Restoring Gimbal Adapter");
            startAdapter(gimbalApiKey);
        }
    }

    /**
     * Starts the adapter.
     * <p>
     * b>Note:</b> The adapter will fail to listen for places if the application does not have proper
     * permissions. Use {@link #isPermissionGranted()} to check for permissions and {@link #startWithPermissionPrompt(String, PermissionResultCallback)}.
     * to prompt the user for permissions while starting the adapter.
     *
     * @param gimbalApiKey The Gimbal API key.
     * @return {@code true} if the adapter started, otherwise {@code false}.
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    public boolean start(@NonNull String gimbalApiKey) {
        startAdapter(gimbalApiKey);
        return Gimbal.isStarted();
    }

    /**
     * Prompts for permission for ACCESS_FINE_LOCATION before starting the adapter.
     * <p>
     * b>Note:</b> You should only call this from a foregrounded activity. This will prompt the user
     * for permissions even if the application is currently in the background.
     *
     * @param gimbalApiKey The Gimbal API key.
     */
    public void startWithPermissionPrompt(@NonNull final String gimbalApiKey) {
        startWithPermissionPrompt(gimbalApiKey, null);
    }

    /**
     * Prompts for permission for ACCESS_FINE_LOCATION before starting the adapter.
     * <p>
     * b>Note:</b> You should only call this from a foregrounded activity. This will prompt the user
     * for permissions even if the application is currently in the background.
     *
     * @param gimbalApiKey The Gimbal API key.
     * @param callback     Optional callback to get the result of the permission prompt.
     */
    public void startWithPermissionPrompt(@NonNull final String gimbalApiKey, @Nullable final PermissionResultCallback callback) {
        requestPermissionsTask = new RequestPermissionsTask(context.getApplicationContext(), new PermissionResultCallback() {
            @Override
            public void onResult(boolean enabled) {
                if (enabled) {
                    //noinspection MissingPermission
                    startAdapter(gimbalApiKey);
                }

                if (callback != null) {
                    callback.onResult(enabled);
                }
            }
        });

        requestPermissionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ACCESS_FINE_LOCATION);
    }


    private void startAdapter(@NonNull String gimbalApiKey) {
        if (isAdapterStarted) {
            return;
        }

        preferences.edit()
                .putString(API_KEY_PREFERENCE, gimbalApiKey)
                .putBoolean(STARTED_REFERENCE, true)
                .apply();

        Gimbal.setApiKey((Application) context.getApplicationContext(), gimbalApiKey);
        PlaceManager.getInstance().addListener(placeEventListener);
        updateDeviceAttributes();

        Gimbal.setApiKey((Application) context.getApplicationContext(), gimbalApiKey);
        Gimbal.start();
        PlaceManager.getInstance().addListener(placeEventListener);
        updateDeviceAttributes();

        Log.i(TAG, String.format("Gimbal Adapter started. Gimabl.isStarted: %b, Gimbal application instance identifier: %s", Gimbal.isStarted(), Gimbal.getApplicationInstanceIdentifier()));
        isAdapterStarted = true;
    }

    /**
     * Stops the adapter.
     */
    public void stop() {
        if (requestPermissionsTask != null) {
            requestPermissionsTask.cancel(true);
        }

        Gimbal.stop();
        PlaceManager.getInstance().removeListener(placeEventListener);
        isAdapterStarted = false;

        preferences.edit()
                .putBoolean(STARTED_REFERENCE, false)
                .apply();

        Log.i(TAG, "Adapter Stopped");
    }

    /**
     * Check if the adapter is started or not.
     */
    public boolean isStarted() {
        return Gimbal.isStarted();
    }

    /**
     * Checks if the application has been granted ACCESS_FINE_LOCATION for Gimbal.
     *
     * @return {@code true} if permissions have been granted, otherwise {@code false}.
     */
    public boolean isPermissionGranted() {
        if (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }


    /**
     * Updates Gimbal and Urban Airship device attributes.
     */
    void updateDeviceAttributes() {
        String gimbalApiKey = preferences.getString(API_KEY_PREFERENCE, null);
        if (gimbalApiKey != null) {

            Map<String, String> deviceAttributes = new HashMap<>();

            if (DeviceAttributesManager.getInstance().getDeviceAttributes() != null
                    && DeviceAttributesManager.getInstance().getDeviceAttributes().size() > 0) {
                deviceAttributes.putAll(DeviceAttributesManager.getInstance().getDeviceAttributes());
            }

            if (UAirship.shared().getNamedUser().getId() != null) {
                deviceAttributes.put(GIMBAL_UA_NAMED_USER_ID, UAirship.shared().getNamedUser().getId());
            }

            if (UAirship.shared().getPushManager().getChannelId() != null) {
                deviceAttributes.put(GIMBAL_UA_CHANNEL_ID, UAirship.shared().getPushManager().getChannelId());
            }

            if (deviceAttributes.size() > 0) {
                DeviceAttributesManager.getInstance().setDeviceAttributes(deviceAttributes);
            }

            String gimbalInstanceId = Gimbal.getApplicationInstanceIdentifier();
            if (gimbalInstanceId != null) {
                UAirship.shared().getAnalytics().editAssociatedIdentifiers().addIdentifier(UA_GIMBAL_APPLICATION_INSTANCE_ID, gimbalInstanceId).apply();
            }
        }
    }

    private class RequestPermissionsTask extends AsyncTask<String, Void, Boolean> {

        private final Context context;
        private PermissionResultCallback callback;


        RequestPermissionsTask(Context context, PermissionResultCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(String... permissions) {
            int[] result = HelperActivity.requestPermissions(context, permissions);
            for (int element : result) {
                if (element == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (callback != null) {
                callback.onResult(result);
            }
        }
    }

}