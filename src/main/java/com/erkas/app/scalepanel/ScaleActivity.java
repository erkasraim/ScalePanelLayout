package com.erkas.app.scalepanel;

import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class ScaleActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_scale);

        final ScalePanelLayout layout = (ScalePanelLayout) findViewById(R.id.sliding_layout);
        layout.setPanelScaleListener(new ScalePanelLayout.PanelScaleListener() {
            @Override
            public void onPanelScale(View panel, float slideOffset) {
//                if (slideOffset < 0.2) {
//                    if (getSupportActionBar().isShowing()) {
//                        getSupportActionBar().hide();
//                    }
//                } else {
//                    if (!getSupportActionBar().isShowing()) {
//                        getSupportActionBar().show();
//                    }
//                }
            }

            @Override
            public void onPanelExpanded(View panel) {

            }

            @Override
            public void onPanelCollapsed(View panel) {

            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scale, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
