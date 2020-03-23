package com.zy.dynamicpermissionlib;

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
public class ZyPermissionConstant {


    public static final int PERMISSIONS_REQUEST_CODE = 1;


    /**
     * The names of the requested permissions.
     * <p>
     * <strong>Type:</strong> String[]
     * </p>
     */
    public static final String EXTRA_REQUEST_PERMISSIONS_NAMES =
            "android.content.pm.extra.REQUEST_PERMISSIONS_NAMES";

    /**
     * The results from the permissions request.
     * <p>
     * <strong>Type:</strong> int[] of #PermissionResult
     * </p>
     */
    public static final String EXTRA_REQUEST_PERMISSIONS_RESULTS
            = "android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS";

}
