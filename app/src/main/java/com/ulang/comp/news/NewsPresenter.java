package com.ulang.comp.news;


import com.ulang.model.core.IMNewsController;
import com.ulang.model.models.NResult;

import rx.Subscription;
import rx.functions.Action1;

public class NewsPresenter implements NewsContract.Presenter {

    NewsContract.View view;
    IMNewsController module;
    private Subscription subscription;


    NewsPresenter(IMNewsController module, NewsContract.View view) {
        this.module = module;
        this.view = view;
        view.setPresenter(this);
        view.onShowTitle();
    }


    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onLoadNewsList() {
        subscription = module.getNews().subscribe(new Action1<NResult>() {
            @Override
            public void call(NResult result) {
                view.onShowNewsList(result);

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });

    }
}
