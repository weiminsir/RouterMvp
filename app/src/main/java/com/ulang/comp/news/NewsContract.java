package com.ulang.comp.news;


import com.ulang.model.BasePresenter;
import com.ulang.model.BaseView;
import com.ulang.model.models.NResult;

public class NewsContract {


    public interface View extends BaseView<Presenter> {

        void onShowTitle();
        void onShowNewsList(NResult result);

    }

    public interface Presenter extends BasePresenter {
        void onLoadNewsList();

    }
}
