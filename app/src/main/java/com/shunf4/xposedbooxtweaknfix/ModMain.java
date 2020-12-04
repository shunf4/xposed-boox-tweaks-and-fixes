package com.shunf4.xposedbooxtweaknfix;

import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModMain implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);

            // Fix crash on touching NavBar buttons
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
                        // XposedBridge.log("xbtnf: before MotionEvent.recycle: " + stackTraceStr);
                        XposedBridge.log("xbtnf: before MotionEvent.recycle: " +
                                "target found. fix!");
                        mhparam.setResult(null);
                    }
                }
            });

            // Make Alt-Tab call Overview (Recents)
            XposedHelpers.findAndHookMethod("com.android.quickstep.TouchInteractionService$3", lpparam.classLoader, "onOverviewShown", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
                    boolean triggeredFromAltTab = (Boolean) mhparam.args[0];
                    if (triggeredFromAltTab) {
                        XposedBridge.log("xbtnf: triggered Overview from AltTab. starting Overview");
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.recents.OnyxRecentsActivity"));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        AndroidAppHelper.currentApplication().startActivity(intent);
                    }
                }
            });
        }

        // Make system apps optimizable
        {
            if (lpparam.packageName.equals("com.onyx.floatingbutton")) {
                XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);


                XposedHelpers.findAndHookMethod("com.onyx.android.sdk.eac.v2.EACManager", lpparam.classLoader, "inEACWhiteList", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
                        XposedBridge.log("xbtnf: inEACWhiteList(): fix!");
                        if ((Boolean) mhparam.getResult()) {
                            Toast.makeText(AndroidAppHelper.currentApplication(), "Target is an app that should be exempt from optimization. " +
                                    "Any modification to this app's optimization config may cause irreversible error.", Toast.LENGTH_LONG).show();
                        }
                        mhparam.setResult(Boolean.FALSE);
                    }
                });

                XposedHelpers.findAndHookMethod("com.onyx.android.sdk.eac.v2.EACManager", lpparam.classLoader, "isEACEnabledApp", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                        XposedBridge.log("xbtnf: isEACEnabledApp(): fix!");
                        mhparam.setResult(Boolean.FALSE);
                    }
                });
            }

            if (lpparam.packageName.equals("android")) {
                XposedHelpers.findAndHookMethod("android.onyx.optimization.OECService", lpparam.classLoader, "isPackageInEACWhiteSet", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                        XposedBridge.log("xbtnf: OECService.isPackageInEACWhiteSet(): fix!");
                        mhparam.setResult(Boolean.FALSE);
                    }
                });
            }
        }
    }
}
