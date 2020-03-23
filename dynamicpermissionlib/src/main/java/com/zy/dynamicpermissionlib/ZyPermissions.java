package com.zy.dynamicpermissionlib;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
public class ZyPermissions {

    private static final String TAG = "ZyPermissions";

    private final Set<String> mPendingRequests = new HashSet<String>(1);
    private final Set<String> mPermissions = new HashSet<String>(1);
    private final List<ZyPermissionsResultAction> mPendingActions = new ArrayList<ZyPermissionsResultAction>(1);

    private ZyPermissionCompatDelegate mPermissionCompatDelegate = new ZyPermissionCompatDelegate();

    private static class SpaPermissionsHolder {
        private static final ZyPermissions INSTANCE = new ZyPermissions();
    }

    private ZyPermissions() {
        initializePermissionsMap();
    }

    public static ZyPermissions getInstance() {
        return SpaPermissionsHolder.INSTANCE;
    }

    /**
     * This method uses reflection to read all the permissions in the Manifest class.
     * This is necessary because some permissions do not exist on older versions of Android,
     * since they do not exist, they will be denied when you check whether you have permission
     * which is problematic since a new permission is often added where there was no previous
     * permission required. We initialize a Set of available permissions and check the set
     * when checking if we have permission since we want to know when we are denied a permission
     * because it doesn't exist yet.
     */
    private synchronized void initializePermissionsMap() {
        Field[] fields = Manifest.permission.class.getFields();
        for (Field field : fields) {
            String name = null;
            try {
                name = (String) field.get("");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not access field", e);
            }
            mPermissions.add(name);
        }
    }

    /**
     * This method adds the {@link ZyPermissionsResultAction} to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the ZyPermissionsResultAction object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the action to be executed.
     * @param action      the action to add to the current list of pending actions.
     */
    private synchronized void addPendingAction(@NonNull String[] permissions,
                                               @Nullable ZyPermissionsResultAction action) {
        if (action == null) {
            return;
        }
        action.registerPermissions(permissions);
        mPendingActions.add(action);
    }

    /**
     * This method removes a pending action from the list of pending actions.
     * It is used for cases where the permission has already been granted, so
     * you immediately wish to remove the pending action from the queue and
     * execute the action.
     *
     * @param action the action to remove
     */
    private synchronized void removePendingAction(@Nullable ZyPermissionsResultAction action) {
        for (Iterator<ZyPermissionsResultAction> iterator = mPendingActions.iterator();
             iterator.hasNext(); ) {
            ZyPermissionsResultAction weakRef = iterator.next();
            if (weakRef == action || weakRef == null) {
                iterator.remove();
            }
        }
    }

    /**
     * This static method can be used to check whether or not you have several specific permissions.
     * It is simpler than checking using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * for each permission and will simply return a boolean whether or not you have all the permissions.
     * If you pass in a null Context object, it will return false as otherwise it cannot check the
     * permission. However, the Activity parameter is nullable so that you can pass in a reference
     * that you are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context     the Context necessary to check the permission
     * @param permissions the permissions to check
     * @return true if you have been granted all the permissions, false otherwise
     */
    public synchronized boolean hasAllPermissions(@Nullable Context context, @NonNull String[] permissions) {
        if (context == null) {
            return false;
        }
        boolean hasAllPermissions = true;
        for (String perm : permissions) {
            hasAllPermissions &= hasPermission(context, perm);
        }
        return hasAllPermissions;
    }

    /**
     * This static method can be used to check whether or not you have a specific permission.
     * It is basically a less verbose method of using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * and will simply return a boolean whether or not you have the permission. If you pass
     * in a null Context object, it will return false as otherwise it cannot check the permission.
     * However, the Activity parameter is nullable so that you can pass in a reference that you
     * are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context    the Context necessary to check the permission
     * @param permission the permission to check
     * @return true if you have been granted the permission, false otherwise
     */
    public boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        return context != null && (checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * This method is the latest method to apply for permission. It only needs to receive the result in ZyPermissionsResultAction. It is not necessary to receive the result in onRequestPermissionsResult method in Activity / Fragment.
     * Step：
     * 1. We receive the result through ActivityCompat.PermissionCompatDelegate. Here we need to call the hidden gray api reflection: buildRequestPermissionsIntent. Gray API list reference：https://android.googlesource.com/platform/frameworks/base/+/pie-release/config/hiddenapi-light-greylist.txt
     * 2. If the first step fails, such as a reflection call to a hidden gray API exception. Then start a ZyPermissionsFragment, request permissions through the empty ZyPermissionsFragment, and receive the result.
     *
     * @param fragmentActivity the fragmentActivity necessary to request the permissions.
     * @param permissions      the list of permissions to request for the {@link ZyPermissionsResultAction}.
     * @param action           the ZyPermissionsResultAction to notify when the permissions are granted or denied.
     */
    public synchronized void requestPermissions(@Nullable FragmentActivity fragmentActivity,
                                                @NonNull String[] permissions,
                                                @Nullable ZyPermissionsResultAction action) {
        if (fragmentActivity == null) {
            return;
        }
        this.mPendingActions.clear();
        this.mPendingRequests.clear();
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(fragmentActivity, permissions, action);
        } else {
            List<String> permList = getPermissionsListToRequest(fragmentActivity, permissions, action);
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action);
            } else {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                ActivityCompat.setPermissionCompatDelegate(mPermissionCompatDelegate);
                if (!mPermissionCompatDelegate.requestPermissions(fragmentActivity, permsToRequest, ZyPermissionConstant.PERMISSIONS_REQUEST_CODE)) {
                    ActivityCompat.setPermissionCompatDelegate(null);
                    requestPermissionsFromFragment(fragmentActivity, permissions);
                }
            }
        }
    }

    public synchronized void requestPermissions(@Nullable Activity activity,
                                                @NonNull String[] permissions,
                                                @Nullable ZyPermissionsResultAction action) {
        if (activity == null) {
            return;
        }
        this.mPendingActions.clear();
        this.mPendingRequests.clear();
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(activity, permissions, action);
        } else {
            List<String> permList = getPermissionsListToRequest(activity, permissions, action);
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action);
            } else {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                ActivityCompat.setPermissionCompatDelegate(mPermissionCompatDelegate);
                if (!mPermissionCompatDelegate.requestPermissions(activity, permsToRequest, ZyPermissionConstant.PERMISSIONS_REQUEST_CODE)) {
                    ActivityCompat.setPermissionCompatDelegate(null);
                    requestPermissions(activity, permissions, action);
                }
            }
        }
    }

    /**
     * This method is the latest method to apply for permission. It only needs to receive the result in ZyPermissionsResultAction. It is not necessary to receive the result in onRequestPermissionsResult method in Activity / Fragment.
     * Step：
     * 1. We receive the result through ActivityCompat.PermissionCompatDelegate. Here we need to call the hidden gray API reflection: buildRequestPermissionsIntent. Gray API list reference：https://android.googlesource.com/platform/frameworks/base/+/pie-release/config/hiddenapi-light-greylist.txt
     * 2. If the first step fails, such as a reflection call to a hidden gray API exception. Then start a ZyPermissionsFragment, request permissions through the empty ZyPermissionsFragment, and receive the result.
     *
     * @param fragment    the fragment necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link ZyPermissionsResultAction}.
     * @param action      the ZyPermissionsResultAction to notify when the permissions are granted or denied.
     */
    public synchronized void requestPermissions(@Nullable Fragment fragment,
                                                @NonNull String[] permissions,
                                                @Nullable ZyPermissionsResultAction action) {
        if (fragment == null) {
            return;
        }
        requestPermissions(fragment.getActivity(), permissions, action);
    }

    private void requestPermissionsFromFragment(FragmentActivity fragmentActivity, String[] permissions) {
        ZyPermissionsFragment permissionsFragment = getPermissionsFragment(fragmentActivity.getSupportFragmentManager());
        if (permissionsFragment.isAdded()) {
            permissionsFragment.requestPermissions(permissions, ZyPermissionConstant.PERMISSIONS_REQUEST_CODE);
        } else {
            permissionsFragment.setPreRequestPermissions(permissions);
        }
    }


    private ZyPermissionsFragment getPermissionsFragment(@NonNull final FragmentManager fragmentManager) {
        ZyPermissionsFragment permissionsFragment = findPermissionsFragment(fragmentManager);
        boolean isNewInstance = permissionsFragment == null;
        if (isNewInstance) {
            permissionsFragment = new ZyPermissionsFragment();
            fragmentManager
                    .beginTransaction()
                    .add(permissionsFragment, TAG)
                    .commit();
        }
        return permissionsFragment;
    }

    private ZyPermissionsFragment findPermissionsFragment(@NonNull FragmentManager fragmentManager) {
        return (ZyPermissionsFragment) fragmentManager.findFragmentByTag(TAG);
    }


    /**
     * This method notifies the PermissionsManager that the permissions have change. If you are making
     * the permissions requests using an Activity, then this method should be called from the
     * Activity callback onRequestPermissionsResult() with the variables passed to that method. If
     * you are passing a Fragment to make the permissions request, then you should call this in
     * the {@link Fragment#onRequestPermissionsResult(int, String[], int[])} method.
     * It will notify all the pending ZyPermissionsResultAction objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed.
     * @param results     the values for each permission.
     */
    public synchronized void notifyPermissionsChange(Context context, @NonNull String[] permissions, @NonNull int[] results) {
        int size = permissions.length;
        if (results.length < size) {
            size = results.length;
        }
        Iterator<ZyPermissionsResultAction> iterator = mPendingActions.iterator();
        while (iterator.hasNext()) {
            ZyPermissionsResultAction action = iterator.next();
            // 保持原逻辑，在第一个拒绝后不在检查下一个权限，但是新增回调，通知权限情况
            boolean handled = false;
            for (int n = 0; n < size; n++) {
                if (action != null) {
                    action.onRequestPermissionsResult(ZyPermissionConstant.PERMISSIONS_REQUEST_CODE, permissions, results);
                    if (!handled) {
                        handled = checkMiPhoneResult(context, action, permissions[n], results[n]);
                    }
                }
            }
        }
        for (int n = 0; n < size; n++) {
            mPendingRequests.remove(permissions[n]);
        }
    }

    public boolean CheckIsForbiddenReq(Activity activity, String permissionName)
    {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionName);
    }

    /**
     * When request permissions on devices before Android M (Android 6.0, API Level 23)
     * Default granted
     *
     * @param activity    the activity to check permissions
     * @param permissions the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     */
    private void doPermissionWorkBeforeAndroidM(@NonNull Activity activity,
                                                @NonNull String[] permissions,
                                                @Nullable ZyPermissionsResultAction action) {
        for (String perm : permissions) {
            if (action != null) {
//                if (!mPermissions.contains(perm)) {
//                    action.onResult(perm, ZyPermissionsEnum.NOT_FOUND);
//                } else if (ActivityCompat.checkSelfPermission(activity, perm)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    action.onResult(perm, ZyPermissionsEnum.DENIED);
//                } else {
                action.onResult(perm, ZyPermissionsEnum.GRANTED);
//                }
            }
        }
    }

    /**
     * Filter the permissions list:
     * If a permission is not granted, add it to the result list
     * if a permission is granted, do the granted work, do not add it to the result list
     *
     * @param activity    the activity to check permissions
     * @param permissions all the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     * @return a list of permissions names that are not granted yet
     */
    @NonNull
    private List<String> getPermissionsListToRequest(@NonNull Activity activity,
                                                     @NonNull String[] permissions,
                                                     @Nullable ZyPermissionsResultAction action) {
        List<String> permList = new ArrayList<String>(permissions.length);
        for (String perm : permissions) {
            if (!mPermissions.contains(perm)) {
                if (action != null) {
                    action.onResult(perm, ZyPermissionsEnum.NOT_FOUND);
                }
            } else if (checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                if (!mPendingRequests.contains(perm)) {
                    permList.add(perm);
                }
            } else {
                if (action != null) {
                    action.onResult(perm, ZyPermissionsEnum.GRANTED);
                }
            }
        }
        return permList;
    }

    private boolean checkMiPhoneResult(Context context, ZyPermissionsResultAction action, String permission, int result) {
        if (checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return action.onResult(permission, ZyPermissionsEnum.GRANTED);
        } else {
            return action.onResult(permission, ZyPermissionsEnum.DENIED);
        }
    }

    private List<String> LocationPermissions = Arrays.asList(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION);


    /**
     * XiaoMi phone's permission Manager is special，try using AppOpsManager to judge whether its permission has been granted
     * this method is just for permission-group : phone & location.
     * for other phones and all sdk-ver < M ,use ActivityCompat.checkSelfPermission is Ok
     *
     * @param context
     * @param permission
     * @return
     */
    private int checkSelfPermission(Context context, String permission) {
        if (context == null) {
            return PackageManager.PERMISSION_DENIED;
        }
        int permissionState = ActivityCompat.checkSelfPermission(context, permission);
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            return permissionState;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(context, permission);
        }
        String op = "";
        if (Manifest.permission.READ_PHONE_STATE.equals(permission)) {
            op = AppOpsManager.OPSTR_READ_PHONE_STATE;
        }
        if (LocationPermissions.contains(permission)) {
            op = AppOpsManager.OPSTR_FINE_LOCATION;
        }
        if (TextUtils.isEmpty(op)) {
            return ActivityCompat.checkSelfPermission(context, permission);
        }
        try {
            AppOpsManager ops = context.getSystemService(AppOpsManager.class);
            int mode = ops.checkOp(op, Process.myUid(), context.getPackageName());
            Log.d(TAG, "mode = " + mode);
            if (mode == AppOpsManager.MODE_ALLOWED) {
                //Accurate judgment for xiaomi
                return PackageManager.PERMISSION_GRANTED;
            } else {
                return PackageManager.PERMISSION_DENIED;
            }
        } catch (Exception e) {
        }
        return ActivityCompat.checkSelfPermission(context, permission);

    }
}
