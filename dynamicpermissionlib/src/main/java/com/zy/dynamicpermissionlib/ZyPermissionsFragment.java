package com.zy.dynamicpermissionlib;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Copyright 2019 kongxiaojun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ZyPermissionsFragment extends Fragment {

    private static final String TAG = "PermissionsFragment";

    private String[] preRequestPermissions = null;

    public ZyPermissionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        ZyPermissions.getInstance().notifyPermissionsChange(getContext(), permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (preRequestPermissions != null) {
            requestPermissions(preRequestPermissions, ZyPermissionConstant.PERMISSIONS_REQUEST_CODE);
        }
        preRequestPermissions = null;
    }

    public void setPreRequestPermissions(String[] preRequestPermissions) {
        this.preRequestPermissions = preRequestPermissions;
    }
}
