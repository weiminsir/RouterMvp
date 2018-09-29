package com.ulang.model.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


import com.ulang.model.IMModelImpl;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class NetworkHelper {

    private static boolean mIsNetAvailable = true;
    private static boolean mIsWifi = true;
    private static boolean mIs3G = false;
    private static BehaviorSubject<Integer> sNetworkTypeSubject = BehaviorSubject.create();
    public static int sShownType = -1;//first-1 normal0 change1
    private static int sLastNetworkType = -10;//-1 means no net

    public static Observable<Integer> getNetworkTypeObservable() {
        return sNetworkTypeSubject.asObservable();
    }

    public static void init(final Context context) {
        //7.0 开始 CONNECTIVITY_ACTION 将无法在后台被接收到
        context.registerReceiver(new NetworkReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        refreshNetState(context);
    }

    public static class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNetState(context);
        }
    }

    static void refreshNetState(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        int networkType;
        if (networkInfo != null && networkInfo.isConnected()) {
            mIsNetAvailable = true;
            networkType = networkInfo.getType();
            mIsWifi = (networkType == ConnectivityManager.TYPE_WIFI || networkType == ConnectivityManager.TYPE_ETHERNET);
            mIs3G = networkType == ConnectivityManager.TYPE_MOBILE;
        } else {
            mIsNetAvailable = false;
            networkType = -1;
            mIsWifi = false;
            mIs3G = false;
        }
        if (sLastNetworkType != networkType) {
            if (sShownType == 0) {
                sShownType = 1;
            }
            sLastNetworkType = networkType;
            sNetworkTypeSubject.onNext(networkType);
        }
    }

    public static boolean isNetAvailable() {
        if (IMModelImpl.getContext() != null) refreshNetState(IMModelImpl.getContext());
        return mIsNetAvailable;
    }

    public static boolean isWifi() {
        if (IMModelImpl.getContext() != null) refreshNetState(IMModelImpl.getContext());
        return mIsWifi;
    }

    public static boolean is3G() {
        if (IMModelImpl.getContext() != null) refreshNetState(IMModelImpl.getContext());
        return mIs3G;
    }
}
