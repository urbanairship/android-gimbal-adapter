/*
 * Copyright 2016 Urban Airship and Contributors
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

import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.urbanairship.UAirship;
import com.urbanairship.location.RegionEvent;
import com.urbanairship.util.DateUtils;
import com.urbanairship.util.HelperActivity;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * GimbalAdapter interfaces Gimbal SDK functionality with Urban Airship services.
 */
public class GimbalAdapter {
    private static final String TAG = "GimbalAdapter";
    private static final String PREFERENCE_NAME = "com.urbanairship.gimbal.preferences";
    private static final String STARTED_PREFERENCE = "com.urbanairship.gimbal.started";
    private static final String API_KEY_PREFERENCE = "com.urbanairship.gimbal.api_key";

    private static final String SOURCE = "Gimbal";

    private static GimbalAdapter instance;
    private final SharedPreferences preferences;
    private final Context context;
    private final List<Listener> listeners = new ArrayList<>();

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
     * Boolean representing the started state of the GimbalAdapter.
     */
    private boolean isStarted = false;

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
     * Restores the last run state. If previously started it will start listening, otherwise
     * it will stop listening. Should be called when the application starts up.
     */
    void restore() {
        boolean started = preferences.getBoolean(STARTED_PREFERENCE, false);
        String apiKey = preferences.getString(API_KEY_PREFERENCE, null);

        try {
            if (apiKey != null && started) {
                //noinspection MissingPermission
                start(apiKey);
            } else {
                stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore gimbal adapter: ", e);
        }
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
        if (isStarted) {
            return true;
        }

        Gimbal.setApiKey((Application) context.getApplicationContext(), gimbalApiKey);

        if (!isPermissionGranted()) {
            Log.e(TAG, "Unable to start adapter, permission denied.");
            return false;
        }

        isStarted = true;
        preferences.edit()
                .putString(API_KEY_PREFERENCE, gimbalApiKey)
                .putBoolean(STARTED_PREFERENCE, true)
                .apply();

        PlaceManager.getInstance().addListener(placeEventListener);
        PlaceManager.getInstance().startMonitoring();
        Log.i(TAG, "Adapter Started. Gimbal application instance identifier: " + Gimbal.getApplicationInstanceIdentifier());
        return true;
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
     * @param callback Optional callback to get the result of the permission prompt.
     */
    public void startWithPermissionPrompt(@NonNull final String gimbalApiKey, @Nullable final PermissionResultCallback callback) {
        RequestPermissionsTask task = new RequestPermissionsTask(context.getApplicationContext(), new PermissionResultCallback() {
            @Override
            public void onResult(boolean enabled) {
                if (enabled) {
                    //noinspection MissingPermission
                    start(gimbalApiKey);
                }

                if (callback != null) {
                    callback.onResult(enabled);
                }
            }
        });

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ACCESS_FINE_LOCATION);
    }

    /**
     * Stops the adapter.
     */
    public void stop() {
        if (!isStarted) {
            return;
        }

        isStarted = false;
        preferences.edit().putBoolean(STARTED_PREFERENCE, false).apply();

        PlaceManager.getInstance().stopMonitoring();
        PlaceManager.getInstance().removeListener(placeEventListener);

        Log.i(TAG, "Adapter Stopped");
    }


    /**
     * Check if the adapter is started or not.
     */
    public boolean isStarted() {
        return isStarted;
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