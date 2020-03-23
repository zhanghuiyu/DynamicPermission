package com.zy.dynamicpermissionlib;

/**
 * Enum class to handle the different states
 * of permissions since the PackageManager only
 * has a granted and denied state.
 */
enum ZyPermissionsEnum {
    GRANTED,
    DENIED,
    NOT_FOUND
}
