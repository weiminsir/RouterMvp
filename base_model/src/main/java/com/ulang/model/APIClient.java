package com.ulang.model;

import com.ulang.model.models.NResult;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public interface APIClient {
    /**
     * 用户逻辑
     */
    @FormUrlEncoded
    @POST("/login")
    Observable<NResult> register(@Field("username") String username,
                                 @Field("password") String password);

    @FormUrlEncoded
    @POST("/register")
    Observable<NResult> login(@Field("username") String username,
                              @Field("password") String password);

    @GET("/user")
    Observable<NResult> getUserStatus(@Path("Path") String uid);

    @GET("/logout")
    Observable<NResult> logout(@Path("Path") String uid);

    /**
     * 新闻逻辑
     */

    @GET("/news")
    Observable<NResult> getNews();


}
