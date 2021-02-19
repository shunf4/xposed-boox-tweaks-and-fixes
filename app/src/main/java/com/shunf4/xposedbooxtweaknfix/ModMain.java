package com.shunf4.xposedbooxtweaknfix;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

public class ModMain implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    private static final String FLAGS_DIRECTORY_PREFIX = "/data/local/xbtnf_";

    private static final boolean shouldDisableNavBarButtonFix =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_navbar_button_fix").exists();
    private static final boolean shouldDisableAltTabOverviewTweak =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_alt_tab_overview_tweak").exists();
    private static final boolean shouldDisableNavBarFormFix =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_navbar_form_fix").exists();
    private static final boolean shouldDisableNavBarColorTweak =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_navbar_color_tweak").exists();
    private static final boolean shouldDisableAllAppsEInkOptimizableTweak =
            new File(FLAGS_DIRECTORY_PREFIX + "disable_all_apps_eink_optimizable_tweak").exists();

    private static void logStackTrace(String prefix) {
        XposedBridge.log("xbtnf: " + prefix + ": stack: " + Log.getStackTraceString(new Exception()));
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);

            if (!shouldDisableNavBarButtonFix) {
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
            }

            if (!shouldDisableAltTabOverviewTweak) {
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

            if (!shouldDisableNavBarFormFix) {
                // Force NavBar to use the normal form (without SwipeUpUI)
                XposedHelpers.findAndHookMethod("com.android.systemui.OverviewProxyService",
                        lpparam.classLoader, "isEnabled", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                                mhparam.setResult(Boolean.FALSE);
                            }
                        });
            }

            if (!shouldDisableNavBarColorTweak) {
//                // Force NavBar to use dark/light icons
//                XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.LightBarController",
//                        lpparam.classLoader, "isLightNavigationBarMode", int.class, new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
//                                XposedBridge.log("xbtnf: this.mHasLightNavigationBar: " + XposedHelpers.getObjectField(mhparam.thisObject, "mHasLightNavigationBar"));
//                                XposedBridge.log("xbtnf: isLightNavigationBarMode: " + mhparam.getResult());
//                                // mhparam.setResult(!shouldNavBarBeDark);
//                            }
//                        });
//
//                XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.LightBarController",
//                        lpparam.classLoader, "isNavigationLight", new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
//                                XposedBridge.log("xbtnf: isNavigationLight: " + mhparam.getResult());
//                                // mhparam.setResult(!shouldNavBarBeDark);
//                            }
//                        });
            }
        }

        if (!shouldDisableAllAppsEInkOptimizableTweak) {
            // Make system apps E-Ink screen optimizable
            if (lpparam.packageName.equals("com.onyx.floatingbutton")) {
                XposedBridge.log("xbtnf: loaded app: " + lpparam.packageName);


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

    public static int rgbToGray(int color) {
        return (((Color.red(color) * 19595) + (38469 * Color.green(color))) + (Color.blue(color) * 7472)) >> 16;
    }

    public static boolean isLightColor(int color) {
        return rgbToGray(color) > 180;
    }

    public static int getColorAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static boolean isRealLightColor(int color, int systemUiVisibility, int windowFlags) {
        return rgbToGray(color) > 140 || (getColorAlpha(color) < 100 && (
                (systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0
                        || (windowFlags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) != 0
        ));
    }

    @SuppressLint("NewApi")
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!shouldDisableNavBarColorTweak) {
            // When NavBar is currently light, make it transparent (to reveal content covered by NavBar in "Note")
            XposedHelpers.findAndHookMethod("com.android.internal.policy.DecorView", null, "calculateNavigationBarColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
//                            XposedBridge.log(String.format("xbtnf: calculateNavigationBarColor: current package: %s", AndroidAppHelper.currentApplication().getPackageName()));
//                            XposedBridge.log(String.format("xbtnf: calculateNavigationBarColor: mNavigationBarColor: %08x", (Integer) XposedHelpers.getObjectField(XposedHelpers.getObjectField(mhparam.thisObject, "mWindow"), "mNavigationBarColor")));
//                            XposedBridge.log(String.format("xbtnf: calculateNavigationBarColor: result: %08x", (Integer) mhparam.getResult()));
                            int systemUiVisibility = (Integer) XposedHelpers.callMethod(
                                    mhparam.thisObject,
                                    "getSystemUiVisibility");
                            int windowFlags = ((Window) XposedHelpers.getObjectField(mhparam.thisObject, "mWindow")).getAttributes().flags;
//                            XposedBridge.log(String.format("xbtnf: calculateNavigationBarColor: systemUiVisibility: %08x, windowFlags: %08x", systemUiVisibility, windowFlags));
                            if (isRealLightColor((Integer) mhparam.getResult(), systemUiVisibility, windowFlags)) {
                                if (AndroidAppHelper.currentApplication().getPackageName().equals("com.onyx.android.note")) {
                                    mhparam.setResult(
                                            Color.TRANSPARENT
                                    );
                                }
//                                XposedBridge.log("xbtnf: isRealLightColor, setSystemUiVisibility!");
                                XposedHelpers.callMethod(
                                        mhparam.thisObject,
                                        "setSystemUiVisibility",
                                        systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                                );
                            }
                        }
                    });
        }
    }

    static final File navBarLayoutConfigFile = new File(FLAGS_DIRECTORY_PREFIX + "navbar_layout");

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        // Replace navBar layout
        if (resparam.packageName.equals("com.android.systemui")) {
            XposedBridge.log("xbtnf: loaded app resource: " + resparam.packageName);

            if (navBarLayoutConfigFile.exists()) {
                try {
                    String navBarLayout = new String(Files.readAllBytes(navBarLayoutConfigFile.toPath()), StandardCharsets.US_ASCII);
                    navBarLayout = navBarLayout.trim();

                    if (!navBarLayout.equals("")) {
                        resparam.res.setReplacement(
                                "com.android.systemui",
                                "string",
                                "config_navBarLayout",
                                navBarLayout);
                    }
                } catch (Exception e) {
                    XposedBridge.log("xbtnf: on tweaking NavBar layout: " + e.toString());
                }
            } else {
                resparam.res.setReplacement(
                        "com.android.systemui",
                        "string",
                        "config_navBarLayout",
                        "left[.5W],back[1WC];space[1W],home[1WC],space[1W];recent[1WC],right[.5W]");
            }
        }
    }
}
