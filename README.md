# Facebook Account Kit and Firebase Authentication Sample
This Android project a sample of using Facebook Account Kit with Firebase Authentication for this
[tutorial](https://medium.com/@shepeliev/how-to-make-facebook-account-kit-and-firebase-authentication-get-along-db3e9e89a595).

## Before Building
You should set Cloud Functions Endpoint in the ```app/build.gradle```

```groove
buildConfigField "String", "CLOUD_FUNCTIONS_ENDPOINT", '"<PUT YOUR ENDPOINT HERE>"'
```

and add two string values:
```xml
<string name="facebook_app_id">FACEBOOK_APP_ID</string>
<string name="account_kit_client_token">FACEBOOK_ACCOUNT_KIT_CLIENT_TOKEN</string>
```

and of course you should connect the project to [Firebase](https://firebase.google.com/docs/android/setup).