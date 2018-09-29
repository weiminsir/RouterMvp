package com.ulang.comp.router;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ulang.comp.R;
import com.ulang.comp.router.annotation.Router;
import com.ulang.comp.MainActivity;
import com.ulang.comp.BaseFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;


/**
 * Created by WangQi on 2017/2/27.
 */

public class Routers {

    private static final String TAG = "Routers";

    private volatile static Routers INSTANCE;
    private final List<OpenInterceptor> openInterceptors;
    private final PublishSubject<OpenRequest> openSubject;
    private static Observable<WeakReference<Activity>> sActivityObservable;

    public static void setActivityObservable(Observable<WeakReference<Activity>> activityObservable) {
        sActivityObservable = activityObservable;
    }

    public static Routers instance() {
        if (INSTANCE == null) {
            synchronized (Routers.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Routers();
                }
            }
        }
        return INSTANCE;
    }

    public static void init(Observable<Activity> activityObservable) {

    }

    private Routers() {
        openInterceptors = new ArrayList<>();
        // for perform some action(not page jump) before login
        addInterceptor(new OpenInterceptor() {
            @Override
            public RouterNode intercept(RouterNode node) {
                if (node instanceof FakeNodeLogin) {

                    if (((FakeNodeLogin) node).canCall()) {
                        try {
                            RouterNode calledNode = ((FakeNodeLogin) node).call();
                            if (calledNode != null) {
                                return calledNode;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, e.getMessage());
                        }
                    } else {
                        ((FakeNodeLogin) node).run();
                    }
                    return OpenRequest.EMPTY_NODE;
                } else {
                    return node;
                }
            }
        });

        openSubject = PublishSubject.create();
        openSubject.delay(new Func1<OpenRequest, Observable<OpenRequest>>() {
            @Override
            public Observable<OpenRequest> call(OpenRequest openRequest) {
                return Observable.just(openRequest);
            }
        }).throttleFirst(400, TimeUnit.MILLISECONDS).flatMap(new Func1<OpenRequest, Observable<OpenRequest>>() {
            @Override
            public Observable<OpenRequest> call(OpenRequest openRequest) {
                return Observable.just(openRequest).map(new Func1<OpenRequest, OpenRequest>() {
                    @Override
                    public OpenRequest call(OpenRequest openRequest) {
                        for (OpenInterceptor interceptor : openInterceptors) {
                            openRequest.node = interceptor.intercept(openRequest.node);
                        }
                        return openRequest;
                    }
                }).onErrorResumeNext(new Func1<Throwable, Observable<? extends OpenRequest>>() {
                    @Override
                    public Observable<? extends OpenRequest> call(Throwable throwable) {
                        return Observable.just(OpenRequest.EMPTY);
                    }
                }).filter(new Func1<OpenRequest, Boolean>() {
                    @Override
                    public Boolean call(OpenRequest openRequest) {
                        return openRequest != OpenRequest.EMPTY && openRequest.node != OpenRequest.EMPTY_NODE;
                    }
                });
            }
        }).withLatestFrom(sActivityObservable, new Func2<OpenRequest, WeakReference<Activity>, OpenRequest>() {
            @Override
            public OpenRequest call(OpenRequest openRequest, WeakReference<Activity> activityRef) {
                openRequest.setContext(activityRef.get());
                return openRequest;
            }
        }).subscribe(new Action1<OpenRequest>() {
            @Override
            public void call(OpenRequest openRequest) {
                if (openRequest.getContext() == null) return;
                if (openRequest.node.isFragment()) {
                    try {
                        gotoFragment(((AppCompatActivity) openRequest.getContext()).getSupportFragmentManager(), openRequest.node.getName(), openRequest.node.wrapBundle(), R.id.fragment_stack, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                } else {

                    gotoActivity(openRequest.getContext(), openRequest.node.getName(), openRequest.node.wrapBundle(), openRequest.requestCode, null);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
                Log.e(TAG, throwable.getMessage());
            }
        });
    }

    public void addInterceptor(OpenInterceptor interceptor) {
        openInterceptors.add(interceptor);
    }

    public void open(int requestCode, RouterNode node) {
        openSubject.onNext(new OpenRequest(requestCode, node));
    }

    public void open(RouterNode node) {
        openSubject.onNext(new OpenRequest(0, node));
        Log.d("weiminsir"," open(RouterNode node) ");
        Log.d("weiminsir"," open(RouterNode node) ");
    }

    public <T extends BaseFragment> T createFragment(RouterNode<T> node) {
        T fragment = null;
        try {
            fragment = (T) Class.forName(node.getName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassCastException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        if (fragment == null) return null;

        fragment.setArguments(node.wrapBundle());
        return fragment;
    }

    public void bindArgs(Object obj, Bundle bundle) {
        try {
            Router annotation = obj.getClass().getAnnotation(Router.class);
            if (annotation == null) {
//                ((RouterNode) Class.forName(obj.getClass().getName() + "Node").newInstance()).bind(obj, bundle);
                return;
            } else {
                String routerName = annotation.name().substring(0, 1).toUpperCase() + annotation.name().substring(1, annotation.name().length());
                ((RouterNode) Class.forName(obj.getClass().getPackage().getName() + "." + routerName + "Node").newInstance()).bind(obj, bundle);
//                Object instance = Class.forName(obj.getClass().getPackage().getName() + "." + routerName + "Node").newInstance();
//                ((RouterNode)instance ).bind(obj, bundle);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassCastException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }


    private void gotoActivity(Context context, String className, Bundle args,
                              int requestCode, @Nullable ActivityAnim anim) {
        if (context == null) return;
        if (args == null) args = new Bundle();


        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(), className));

        intent.putExtras(args);
        if (className.equals(MainActivity.class.getName())) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (requestCode != 0) {
            ((Activity) context).startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }

//        if (context instanceof Activity) {
//            if (anim != null) {
//                anim.effect((Activity) context);
//            } else {
//                ((Activity) context).overridePendingTransition(R.anim.activity_slide_to_left, R.anim.stay_still);
//            }
//        }
    }

    interface ActivityAnim {
        void effect(Activity activity);
    }


    public void setRootFragment(FragmentActivity activity, Fragment fragment, Bundle args, @IdRes int stackId) {
        if (activity == null) return;
        if (args != null) fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        fragmentTransaction.replace(stackId, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    public void setRootFragment(FragmentActivity activity, Fragment fragment, Bundle args) {
        setRootFragment(activity, fragment, args, R.id.fragment_stack);
    }

    public void setRootFragment(FragmentActivity activity, RouterNode node) {

        BaseFragment fragment = null;
        try {
            fragment = (BaseFragment) Class.forName(node.getName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassCastException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        if (fragment == null) return;

        fragment.setArguments(activity.getIntent().getExtras());

        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        fragmentTransaction.replace(R.id.fragment_stack, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    private void gotoFragment(FragmentManager fragmentManager, String fragmentName, Bundle args,
                              @IdRes int stackId, @Nullable FragmentAnim anim) {
        if (fragmentManager == null) return;

        BaseFragment fragment = null;
        try {
            fragment = (BaseFragment) Class.forName(fragmentName).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassCastException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        if (fragment == null) return;

        if (args != null) fragment.setArguments(args);

        if (fragmentManager.getBackStackEntryCount() > 0) {
            Fragment popMenu = fragmentManager.findFragmentByTag(fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName());
//            if (popMenu instanceof BasePopMenu) {
//                fragmentManager.popBackStack();
//            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (anim != null) {
            anim.effect(fragmentTransaction);
        } else {
            //设置进入方向
            if (fragment.addFromDirection() == BaseFragment.AddFromRight) {
                fragmentTransaction.setCustomAnimations(R.anim.activity_slide_to_left,
                        R.anim.activity_slide_to_right,
                        R.anim.activity_slide_to_left,
                        R.anim.activity_slide_to_right);
            } else if (fragment.addFromDirection() == BaseFragment.AddFromFade) {
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            }
        }

        fragmentTransaction.add(stackId, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    interface FragmentAnim {
        void effect(FragmentTransaction fragmentTransaction);
    }

    private static String getPackageName(String qualifiedName) {
        int i = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, i);
    }

    private static String getClassSimpleName(String qualifiedName) {
        int i = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(i + 1, qualifiedName.length());
    }

    private static class OpenRequest {
        int requestCode;
        RouterNode node;
        @Nullable
        private Context context;

        static final OpenRequest EMPTY = new OpenRequest(0, null);
        static final RouterNode EMPTY_NODE = new RouterNode() {
            @Override
            public void bind(Object target, Bundle bundle) {/*do nothing*/}

            @Override
            public Bundle wrapBundle() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean needLogin() {
                return false;
            }

            @Override
            public boolean isFragment() {
                return false;
            }
        };

        OpenRequest(int requestCode, RouterNode node) {
            this.requestCode = requestCode;
            this.node = node;
        }

        @Nullable
        public Context getContext() {
            return context;
        }

        public void setContext(@Nullable Context context) {
            this.context = context;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OpenRequest that = (OpenRequest) o;

            if (requestCode != that.requestCode) return false;
            return node != null ? node.equals(that.node) : that.node == null;

        }

        @Override
        public int hashCode() {
            int result = requestCode;
            result = 31 * result + (node != null ? node.hashCode() : 0);
            return result;
        }
    }

    public static class FakeNodeLogin implements RouterNode, Runnable, Callable<RouterNode> {

        private Runnable runnable;
        private Callable<RouterNode> callable;

        public FakeNodeLogin(Runnable runnable) {
            this.runnable = runnable;
        }

        public FakeNodeLogin(Callable<RouterNode> callable) {
            this.callable = callable;
        }

        @Override
        public void bind(Object target, Bundle bundle) {

        }

        @Override
        public Bundle wrapBundle() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean needLogin() {
            return true;
        }

        @Override
        public boolean isFragment() {
            return false;
        }

        @Override
        public void run() {
            if (runnable != null) runnable.run();
        }

        @Override
        public RouterNode call() throws Exception {
            if (callable != null) return callable.call();
            return null;
        }

        public boolean canCall() {
            return callable != null;
        }
    }
}
