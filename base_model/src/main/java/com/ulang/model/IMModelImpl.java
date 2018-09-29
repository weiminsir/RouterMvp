package com.ulang.model;

import android.content.Context;


import com.ulang.model.core.IMNewsController;
import com.ulang.model.core.IMUserController;
import com.ulang.model.impl.IMNewsControllerImpl;
import com.ulang.model.impl.IMUserControllerImpl;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public class IMModelImpl {
    private static IMUserController sUserController;
    private static IMNewsController sNewsController;
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    public static void init(Context context) {
        sContext = context;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("http://59.39.58.131")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        APIClient sAPIClient = retrofit.create(APIClient.class);
        sUserController = new IMUserControllerImpl(sAPIClient);
        sNewsController = new IMNewsControllerImpl(sAPIClient);


    }


    public static void destroy() {
        sUserController = null;
        sNewsController = null;
    }

    public static IMUserController getsUserDataController() {
        return sUserController;
    }

    public static IMNewsController getsNewsController() {
        return sNewsController;
    }
}
