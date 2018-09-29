package com.ulang.comp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ulang.comp.router.Routers;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.subjects.PublishSubject;

public abstract class BaseFragment extends android.support.v4.app.Fragment {

    /**
     * 进入方向
     */
    public static final int AddFromRight = 1;
    public static final int AddFromFade = 4;
    public static final int AddFromLeft = 5;
    private PublishSubject<Integer> playResultSubject = PublishSubject.create();

    /**
     * 返回进入方向 子类继承
     *
     * @return 进入方向
     */
    public int addFromDirection() {
        return AddFromRight;
    }



    /**
     * 返回是否可以滑动退出 子类继承
     *
     * @return true 是 false 否
     */


    /**
     * 返回layout 子类继承
     *
     * @return layoutResID
     */
    abstract protected int getLayoutResID();

    private Unbinder unbinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("weiminsir", "my name is " + this.getClass().getSimpleName());

        Routers.instance().bindArgs(this, getArguments());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResID(), container, false);
        unbinder = ButterKnife.bind(this, rootView);

        return rootView;

    }


    @Override
    public void onDestroyView() {
        if (unbinder != null)
            unbinder.unbind();
        super.onDestroyView();
    }


}
