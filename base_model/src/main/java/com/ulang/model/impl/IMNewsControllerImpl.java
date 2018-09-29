package com.ulang.model.impl;

import com.ulang.model.APIClient;
import com.ulang.model.core.IMNewsController;
import com.ulang.model.impl.base.IMBaseController;
import com.ulang.model.models.NResult;

import rx.Observable;

public class IMNewsControllerImpl extends IMBaseController implements IMNewsController {

    public IMNewsControllerImpl(APIClient apiClient) {
        super(apiClient);
    }


    @Override
    public Observable<NResult> getNews() {
        return apiClient.getNews().compose(this.<NResult>applySchedulers());
    }
}
