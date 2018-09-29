package com.ulang.comp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ulang.comp.login.LoginNode;
import com.ulang.comp.news.NewsNode;
import com.ulang.comp.router.Routers;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Routers.instance().open(new LoginNode());
            }
        });
        findViewById(R.id.tv_news).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Routers.instance().open(new NewsNode("今日头条"));
            }
        });


//        Intent intent = new Intent(this, LoginActivity.class);
//        Bundle bundle = new Bundle();
//        bundle.putString("name", "Hello World");
//        intent.putExtras(bundle);
//        startActivity(intent);

    }


}
