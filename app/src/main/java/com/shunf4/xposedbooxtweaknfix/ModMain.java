package com.shunf4.xposedbooxtweaknfix;

import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModMain implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui")) return;

        XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod("android.view.MotionEvent", lpparam.classLoader, "recycle", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                new Exception().printStackTrace(pw);
                String stackTraceStr = sw.toString();

                // XposedBridge.log("xbtnf: before MotionEvent.recycle: " + stackTraceStr);

                StackTraceElement[] stes = Thread.currentThread().getStackTrace();
                StackTraceElement targetSte = null;
                for (StackTraceElement ste : stes) {
                    if (ste.getClassName().contains("com.android.quickstep.TouchInteractionService")
                            && ste.getMethodName().equals("onMotionEvent")) {
                        targetSte = ste;
                        break;
                    }
                }

                if (targetSte == null) {
                    /* XposedBridge.log("xbtnf: before MotionEvent.recycle: " +
                            "com.android.quickstep.TouchInteractionService$3.onMotionEvent " +
                            "not found in stack trace"); */
                } else {
                    XposedBridge.log("xbtnf: before MotionEvent.recycle: " + stackTraceStr);
                    XposedBridge.log("xbtnf: before MotionEvent.recycle: " +
                            "target found. fix!");
                    mhparam.setResult(null);
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.android.quickstep.TouchInteractionService$3", lpparam.classLoader, "onOverviewShown", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
                boolean triggeredFromAltTab = (Boolean) mhparam.args[0];
                if (triggeredFromAltTab) {
                    XposedBridge.log("xbtnf: triggered Overview from AltTab. start Overview");
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.recents.OnyxRecentsActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                }
            }
        });
    }
}
