package com.jack.fpsmonitor.ui;

import android.os.Build;
import android.view.WindowManager;

/**
 * 悬浮窗权限兼容
 *
 * @author jack
 * @since 2020/6/17
 */
public final class PermissionCompat {

    private PermissionCompat() {
    }

    /**
     * 添加悬浮窗
     */
    public static int getFlag() {
        int permissionFlag;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissionFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            permissionFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return permissionFlag;
    }
}
