package com.ulang.comp.router;

import android.os.Bundle;

/**
 * Created by WangQi on 2017/2/27.
 */

public interface RouterNode<T> {
    void bind(T target, Bundle bundle);

    Bundle wrapBundle();

    String getName();

    boolean needLogin();

    boolean isFragment();
}
