package com.ulang.model;

/**
 * Created by WangQi on 2016/12/12.
 */

public interface BaseView<T extends BasePresenter> {
    void setPresenter(T presenter);
}
