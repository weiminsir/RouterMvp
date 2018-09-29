package com.ulang.comp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.ulang.comp.router.Routers;
import com.ulang.model.IMModelImpl;

import java.lang.ref.WeakReference;

import rx.subjects.BehaviorSubject;

public class UlangApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        IMModelImpl.init(this);

        final BehaviorSubject<WeakReference<Activity>> activitySubject = BehaviorSubject.create();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {/*do nothing*/}

            @Override
            public void onActivityStarted(Activity activity) {/*do nothing*/}

            @Override
            public void onActivityResumed(Activity activity) {
                activitySubject.onNext(new WeakReference<>(activity));
            }

            @Override
            public void onActivityPaused(Activity activity) {/*do nothing*/}

            @Override
            public void onActivityStopped(Activity activity) {/*do nothing*/}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {/*do nothing*/}

            @Override
            public void onActivityDestroyed(Activity activity) {/*do nothing*/}
        });
        Routers.setActivityObservable(activitySubject.asObservable());

    }
}
