package com.curiousily.ipoli;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 7/31/15.
 */
public class QuestDetailActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest_detail);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.md_green_500));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overrideExitAnimation();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overrideExitAnimation();
    }

    private void overrideExitAnimation() {
        overridePendingTransition(R.anim.reverse_slide_in, R.anim.reverse_slide_out);
    }
}
