package com.duoyi.dynamicpermissionset;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.zy.dynamicpermissionlib.ZyDynamicPermissionMain;

public class MainActivity extends AppCompatActivity {

    ZyDynamicPermissionMain permissionMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionMain = new ZyDynamicPermissionMain();
        Button btn1 = findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionMain.ReqPermission(MainActivity.this, Manifest.permission.CAMERA);
            }
        });

        Button btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionMain.ReqPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionMain.ReqPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        });
    }
}
