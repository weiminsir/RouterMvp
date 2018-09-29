package com.ulang.comp.login;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.ulang.comp.BaseFragment;
import com.ulang.comp.R;
import com.ulang.comp.router.annotation.Args;
import com.ulang.comp.router.annotation.Router;
import com.ulang.model.IMModelImpl;

@Router(name = "loginInner")
public class LoginFragment extends BaseFragment implements LoginContract.View {

    @Args
    String name;
    TextView textView;

    private LoginContract.Presenter presenter;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_login;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textView = view.findViewById(R.id.tv_content);
        presenter = new LoginPresenter(IMModelImpl.getsUserDataController(), this);

    }

    @Override
    public void onShowFlag() {
        textView.setText(name);
    }

    @Override
    public void onLoginError(String message) {

    }

    @Override
    public void onLoginSuccess() {

    }

    @Override
    public void setPresenter(LoginContract.Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }


}
