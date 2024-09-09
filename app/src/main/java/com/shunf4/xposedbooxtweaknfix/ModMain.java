package com.shunf4.xposedbooxtweaknfix;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModMain implements IXposedHookLoadPackage, /* IXposedHookZygoteInit ,*/ IXposedHookInitPackageResources {
    private static final String FLAGS_DIRECTORY_PREFIX = "/data/local/xbtnf_";

    private static final boolean shouldDisableAllAppsEInkOptimizableTweak =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_all_apps_eink_optimizable_tweak").exists();

    private static void logStackTrace(String prefix) {
        XposedBridge.log("xbtnf: " + prefix + ": stack: " + Log.getStackTraceString(new Exception()));
    }

    private void readerRecentsHook(XC_MethodHook.MethodHookParam param, String logPrefix) {
        ActivityManager am = (ActivityManager) ((Activity) param.thisObject).getSystemService(Context.ACTIVITY_SERVICE);
        XposedBridge.log("xbtnf: " + logPrefix + ": am: " + (am == null ? "<null>" : am.toString()));
        if (am != null) {
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            XposedBridge.log("xbtnf: " + logPrefix + ": am.getAppTasks: " + tasks == null ? "<null>" : (tasks.size() + " " + tasks));
            if (tasks != null && tasks.size() > 0) {
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo());
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo().baseActivity);
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo().origActivity);
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo().description);
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo().topActivity);
                XposedBridge.log("xbtnf: " + logPrefix + ": taskInfo: " + tasks.get(0).getTaskInfo().baseIntent);
                tasks.get(0).setExcludeFromRecents(false);
                XposedBridge.log("xbtnf: " + logPrefix + ": setExcludeFromRecents: false");
            }
        }

        if (param.args != null && param.args.length >= 1 && param.args[0] != null && param.args[0] instanceof Intent) {
            XposedBridge.log("xbtnf: " + logPrefix + ": do not FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS");
            ((Intent) param.args[0]).setFlags(((Intent) param.args[0]).getFlags() & ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);
        }

        if (lpparam.packageName.equals("com.onyx.kreader")) {
            XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);
//            Activity a = new Activity() {
//                @Override
//                protected void onNewIntent(Intent intent) {
//                    super.onNewIntent(intent);
//                }
//            }
            XposedHelpers.findAndHookMethod("com.onyx.kreader.ui.ReaderHomeActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    readerRecentsHook(param, "onCreate");
                }
            });

            XposedHelpers.findAndHookMethod(Activity.class, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    readerRecentsHook(param, "startActivity");
                }
            });
        }

        if (!shouldDisableAllAppsEInkOptimizableTweak) {
            // Make system apps E-Ink screen optimizable
            if (lpparam.packageName.equals("com.onyx.floatingbutton")) {
                // XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);

                XposedHelpers.findAndHookMethod("com.onyx.android.sdk.eac.v2.EACManager", lpparam.classLoader, "inEACWhiteList", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
                        // XposedBridge.log("xbtnf: inEACWhiteList(): fix!");
                        if ((Boolean) mhparam.getResult()) {
                            try {
                                Toast.makeText(AndroidAppHelper.currentApplication(), "Target is an app that should be exempt from optimization. " +
                                        "Any modification to this app's optimization config may cause irreversible error.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                ;
                            }
                        }
                        mhparam.setResult(Boolean.FALSE);
                    }
                });

                XposedHelpers.findAndHookMethod("com.onyx.android.sdk.eac.v2.EACManager", lpparam.classLoader, "isEACEnabledApp", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                        // XposedBridge.log("xbtnf: isEACEnabledApp(): fix!");
                        mhparam.setResult(Boolean.FALSE);
                    }
                });
            }

            if (lpparam.packageName.equals("android")) {
                XposedHelpers.findAndHookMethod("android.onyx.optimization.OECService", lpparam.classLoader, "isPackageInEACWhiteSet", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                        // XposedBridge.log("xbtnf: OECService.isPackageInEACWhiteSet(): fix!");
                        mhparam.setResult(Boolean.FALSE);
                    }
                });
            }
        }
    }

//    @Override
//    public void initZygote(StartupParam startupParam) throws Throwable {
//
//    }

    static final File navBarLayoutConfigFile = new File(FLAGS_DIRECTORY_PREFIX + "navbar_layout");

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        // Replace navBar layout
        if (resparam.packageName.equals("com.android.systemui")) {
//            if (navBarLayoutConfigFile.exists()) {
//                try {
//                    String navBarLayout = new String(Files.readAllBytes(navBarLayoutConfigFile.toPath()), StandardCharsets.US_ASCII);
//                    navBarLayout = navBarLayout.trim();
//
//                    if (!navBarLayout.equals("")) {
//                        resparam.res.setReplacement(
//                                "com.android.systemui",
//                                "string",
//                                "config_navBarLayout",
//                                navBarLayout);
//                    }
//                } catch (Exception e) {
//                    XposedBridge.log("xbtnf: on tweaking NavBar layout: " + e.toString());
//                }
//            } else {
//                resparam.res.setReplacement(
//                        "com.android.systemui",
//                        "string",
//                        "config_navBarLayout",
//                        "left[.5W],back[1WC];space[1W],home[1WC],space[1W];recent[1WC],right[.5W]");
//            }
        }
    }
}
