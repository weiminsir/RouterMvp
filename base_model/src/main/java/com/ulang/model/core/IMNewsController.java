package com.ulang.model.core;


import com.ulang.model.models.NResult;

import rx.Observable;


public interface IMNewsController {

    Observable<NResult> getNews();

}
