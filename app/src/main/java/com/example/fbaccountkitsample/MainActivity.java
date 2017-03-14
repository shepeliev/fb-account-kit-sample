package com.example.fbaccountkitsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.accountkit.AccessToken;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static int APP_REQUEST_CODE = 99;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CloudFunctions mCloudFunctions;

    public void onLogoutClick(View view) {
        AccountKit.logOut();
        mAuth.signOut();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    Toast.makeText(MainActivity.this, "User signed in: " + user.getUid(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    final AccessToken accessToken = AccountKit.getCurrentAccessToken();
                    if (accessToken != null) {
                        getCustomToken(accessToken);
                    } else {
                        phoneLogin();
                    }
                }
            }
        };

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.CLOUD_FUNCTIONS_ENDPOINT)
                .build();
        mCloudFunctions = retrofit.create(CloudFunctions.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == APP_REQUEST_CODE) {
            handleFacebookLoginResult(resultCode, data);
        }
    }

    private void handleFacebookLoginResult(final int resultCode, final Intent data) {
        final AccountKitLoginResult loginResult =
                data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

        if (loginResult.getError() != null) {
            final String toastMessage = loginResult.getError().getErrorType().getMessage();
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        } else if (loginResult.wasCancelled() || resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Login cancelled");
            finish();
        } else {
            if (loginResult.getAccessToken() != null) {
                Log.d(TAG, "We have logged with FB Account Kit. ID: " +
                        loginResult.getAccessToken().getAccountId());
                getCustomToken(loginResult.getAccessToken());
            } else {
                Log.wtf(TAG, "It should not have been happened");
            }
        }
    }

    private void getCustomToken(final AccessToken accessToken) {
        Log.d(TAG, "Getting custom token for Account Kit access token: " + accessToken.getToken());
        mCloudFunctions.getCustomToken(accessToken.getToken()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        final String customToken = response.body().string();
                        Log.d(TAG, "Custom token: " + customToken);
                        signInWithCustomToken(customToken);
                    } else {
                        Log.e(TAG, response.errorBody().string());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable e) {
                Log.e(TAG, "Request getCustomToken failed", e);
            }
        });
    }

    private void signInWithCustomToken(String customToken) {
        mAuth.signInWithCustomToken(customToken)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "getCustomToken:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getCustomToken", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void phoneLogin() {
        final Intent intent = new Intent(this, AccountKitActivity.class);
        final AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }

}
