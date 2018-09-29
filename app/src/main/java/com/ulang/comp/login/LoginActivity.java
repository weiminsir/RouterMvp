package com.ulang.comp.login;

import android.os.Bundle;

import com.ulang.comp.BaseActivity;
import com.ulang.comp.R;
import com.ulang.comp.router.Routers;
import com.ulang.comp.router.annotation.Router;
import com.ulang.comp.login.LoginFragment;

@Router(name = "login")
public class LoginActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Bundle bundle = new Bundle();
        bundle.putString("name", "success");
        Routers.instance().setRootFragment(this, new LoginFragment(), bundle);
        // 如果activity


    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
