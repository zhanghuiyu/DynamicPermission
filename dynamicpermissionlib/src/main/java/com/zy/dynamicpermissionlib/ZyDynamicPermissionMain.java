package com.zy.dynamicpermissionlib;
import android.app.Activity;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ZyDynamicPermissionMain {
    /**
     * unity项目启动时的的上下文
     */
    private Activity _unityActivity;
    /**
     * 获取unity项目的上下文
     * @return
     */
    Activity getActivity(){
        if(null == _unityActivity) {
            try {
                Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
                Activity activity = (Activity) classtype.getDeclaredField("currentActivity").get(classtype);
                _unityActivity = activity;
            } catch (ClassNotFoundException e) {

            } catch (IllegalAccessException e) {

            } catch (NoSuchFieldException e) {

            }
        }
        return _unityActivity;
    }

    /**
     * 调用Unity的方法
     * @param gameObjectName    调用的GameObject的名称
     * @param functionName      方法名
     * @param args              参数
     * @return                  调用是否成功
     */
    public static boolean callUnity(String gameObjectName, String functionName, String args){
        try {
            Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
            Method method =classtype.getMethod("UnitySendMessage", String.class,String.class,String.class);
            method.invoke(classtype,gameObjectName,functionName,args);
            return true;
        } catch (ClassNotFoundException e) {

        } catch (NoSuchMethodException e) {

        } catch (IllegalAccessException e) {

        } catch (InvocationTargetException e) {

        }
        return false;
    }

    public void UnityReqPermission(String permissionName)
    {
        ReqPermission(getActivity(), permissionName);
    }

    public void ReqPermission(final Activity activity, final String permissionName)
    {
        ZyPermissions.getInstance().requestPermissions(activity, new String[]{permissionName}, new ZyPermissionsResultAction() {
            @Override
            public void onGranted() {
                Toast.makeText(activity, "同意" + permissionName, Toast.LENGTH_SHORT).show();
                callUnity("GameMgr", "OnReqPermissionGrantedCallback", permissionName);
            }

            @Override
            public void onDenied(String permission) {
                if (ZyPermissions.getInstance().CheckIsForbiddenReq(activity, permissionName))
                {
                    Toast.makeText(activity, "拒绝并不可再请求"+ permission, Toast.LENGTH_SHORT).show();
                    callUnity("GameMgr", "OnReqPermissionForbiddenCallback", permission);
                }
                else
                {
                    Toast.makeText(activity, "拒绝"+ permission, Toast.LENGTH_SHORT).show();
                    callUnity("GameMgr", "OnReqPermissionDeniedCallback", permission);
                }

            }
        });
    }
}
