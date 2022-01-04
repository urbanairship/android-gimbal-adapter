# Urban Airship Android Gimbal Adapter

The Urban Airship Gimbal Adapter is a drop-in class that allows users to integrate Gimbal Place events with
Urban Airship.

## Resources
- [Gimbal Developer Guide](https://gimbal.com/doc/android/v4/devguide.html)
- [Gimbal Manager Portal](https://manager.gimbal.com)
- [Urban Airship Getting Started guide](https://docs.airship.com/platform/android/getting-started/)

## Installation

To install it add the following dependency to your application's build.gradle file:
```
   implementation 'com.urbanairship.android:gimbal-adapter:7.3.0'
```

## Set the Gimbal API key

Set the API key during Application#onCreate:
```
   Gimbal.setApiKey(this, "## PLACE YOUR API KEY HERE ##");
```

Alternatively you can have the adapter automatically persist and set the API key on Gimbal when
the app starts:
```
   GimbalAdapter.shared(context).enableGimbalApiKeyManagement("## PLACE YOUR API KEY HERE ##");
```

To stop this behavior, you will need to disable it:
```
   GimbalAdapter.shared(context).disableGimbalApiKeyManagement();
```


## Start the adapter

To start the adapter call:
```
   GimbalAdapter.shared(context).start();
```

Once the adapter is started, it will automatically resume its last state if
the application is restarted in the background. You only need to call start
once.

### Android Marshmallow Permissions

Before the adapter is able to be started on Android M, it must request the location permission
``ACCESS_FINE_LOCATION``. The adapter has convenience methods that you can use to request permissions while
starting the adapter:
```
    GimbalAdapter.shared(context).startWithPermissionPrompt(new GimbalAdapter.PermissionResultCallback() {
        @Override
        public void onResult(boolean enabled) {
            if (enabled) {
                // permission granted and adapter started
            }
        }
    });
```

Alternatively you can follow [requesting runtime permissions](https://developer.android.com/training/permissions/requesting.html)
to manually request the proper permissions. Then once the permissions are granted, call start on the adapter.

## Stopping the adapter

Adapter can be stopped at anytime by calling:
```
   GimbalAdapter.shared(context).stop();
```
