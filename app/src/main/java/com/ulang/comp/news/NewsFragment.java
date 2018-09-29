package com.ulang.comp.news;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.ulang.comp.BaseFragment;
import com.ulang.comp.R;
import com.ulang.comp.router.annotation.Args;
import com.ulang.comp.router.annotation.Router;
import com.ulang.model.IMModelImpl;
import com.ulang.model.models.NResult;

@Router(name = "NewsInner")
public class NewsFragment extends BaseFragment implements NewsContract.View {
    @Args
    String title;
    TextView textView;
    NewsContract.Presenter mPresenter;


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_news;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textView = ((TextView) view.findViewById(R.id.tv_content));
        mPresenter = new NewsPresenter(IMModelImpl.getsNewsController(), this);
        mPresenter.onLoadNewsList();

    }

    @Override
    public void onShowTitle() {
        textView.setText(title);
    }

    @Override
    public void onShowNewsList(NResult result) {


    }

    @Override
    public void setPresenter(NewsContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
