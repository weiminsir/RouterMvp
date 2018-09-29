package com.ulang.comp.news;

import android.os.Bundle;

import com.ulang.comp.BaseActivity;
import com.ulang.comp.R;
import com.ulang.comp.router.Routers;
import com.ulang.comp.router.annotation.Args;
import com.ulang.comp.router.annotation.Router;

@Router(name = "news")
public class NewsActivity extends BaseActivity {


    @Args
    String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Routers.instance().setRootFragment(this,new NewsInnerNode());

    }
}
