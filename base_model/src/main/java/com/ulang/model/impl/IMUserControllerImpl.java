package com.ulang.model.impl;


import com.ulang.model.APIClient;
import com.ulang.model.core.IMUserController;
import com.ulang.model.impl.base.IMBaseController;
import com.ulang.model.models.NResult;

import rx.Observable;

public class IMUserControllerImpl extends IMBaseController implements IMUserController {


    public IMUserControllerImpl(final APIClient apiClient) {
       super(apiClient);
    }

    @Override
    public Observable<NResult> register(String username, String password) {
        return apiClient.register(username, password)
                .compose(this.<NResult>applySchedulers());
    }

    @Override
    public Observable<NResult> login(String username, String password) {
        return apiClient.login(username, password)
                .compose(this.<NResult>applySchedulers());
    }

    @Override
    public Observable<NResult> getUserStatus(String uid) {
        return apiClient.getUserStatus(uid)
                .compose(this.<NResult>applySchedulers());
    }

    @Override
    public Observable<NResult> logout(String uid) {
        return apiClient.logout(uid)
                .compose(this.<NResult>applySchedulers());
    }


}
