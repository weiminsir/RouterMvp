package com.ulang.comp.login;


import com.ulang.model.BasePresenter;
import com.ulang.model.BaseView;

public interface LoginContract {
    interface View extends BaseView<Presenter> {
        void onShowFlag();
        void onLoginError(String message);
        void onLoginSuccess();
    }

    interface Presenter extends BasePresenter {
        void login(String username, String password);

    }
}
