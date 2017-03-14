package com.example.fbaccountkitsample;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CloudFunctions {

    @GET("getCustomToken")
    Call<ResponseBody> getCustomToken(@Query("access_token") String accessToken);
}
