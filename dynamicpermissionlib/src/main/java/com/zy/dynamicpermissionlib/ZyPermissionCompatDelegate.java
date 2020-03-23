package com.zy.dynamicpermissionlib;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;

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
public class ZyPermissionCompatDelegate implements ActivityCompat.PermissionCompatDelegate {
    @Override
    public boolean requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
        try {
            Method method = activity.getPackageManager().getClass().getMethod("buildRequestPermissionsIntent", String[].class);
            Intent intent = (Intent) method.invoke(activity.getPackageManager(), (Object) permissions);
            if (intent != null) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        ActivityCompat.setPermissionCompatDelegate(null);
        String[] permissions = (data != null) ? data.getStringArrayExtra(
                ZyPermissionConstant.EXTRA_REQUEST_PERMISSIONS_NAMES) : new String[0];
        final int[] grantResults = (data != null) ? data.getIntArrayExtra(
                ZyPermissionConstant.EXTRA_REQUEST_PERMISSIONS_RESULTS) : new int[0];
        ZyPermissions.getInstance().notifyPermissionsChange(activity, permissions, grantResults);
        return true;
    }
}
