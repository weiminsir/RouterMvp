package com.ulang.comp;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import com.trello.rxlifecycle.components.RxActivity;
import com.trello.rxlifecycle.components.support.RxFragmentActivity;
import com.ulang.comp.router.Routers;
import com.ulang.comp.router.annotation.Router;

public class BaseActivity extends RxFragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Routers.instance().bindArgs(this, getIntent().getExtras());
    }
}
