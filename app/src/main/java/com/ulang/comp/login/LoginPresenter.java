package com.ulang.comp.login;

import com.ulang.model.core.IMUserController;
import com.ulang.model.models.NResult;

import rx.Subscription;
import rx.functions.Action1;

public class LoginPresenter implements LoginContract.Presenter {
    LoginContract.View view;
    IMUserController module;
    private Subscription subscription;

    public LoginPresenter(IMUserController module, LoginContract.View view) {
        this.module = module;
        this.view = view;
        view.setPresenter(this);
        view.onShowFlag();
    }

    @Override
    public void login(String username, String password) {
        subscription = module.login(username, password).subscribe(new Action1<NResult>() {
            @Override
            public void call(NResult nResult) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                view.onLoginError("登录错误");
            }
        });
    }

    @Override
    public void unsubscribe() {

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void subscribe() {

    }
}
