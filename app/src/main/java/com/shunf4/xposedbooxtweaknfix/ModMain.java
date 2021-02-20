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

class ModColorUtils {
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
        return rgbToGray(color) > 140 || isRealTransparentColor(color, systemUiVisibility, windowFlags);
    }

    public static boolean isRealTransparentColor(int color, int systemUiVisibility, int windowFlags) {
        return (getColorAlpha(color) < 100 && (
                (systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0
                        || (windowFlags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) != 0
        ));
    }
}

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
    private static final boolean shouldNavBarBeAlwaysDark =
            new File(FLAGS_DIRECTORY_PREFIX + "navbar_dark").exists();
    private static final boolean shouldNavBarBeAlwaysLight =
            new File(FLAGS_DIRECTORY_PREFIX + "navbar_light").exists();
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
                // This is caused by an extra ev.recycle() call in com.android.quickstep.TouchInteractionService.mMyBinder#onMotionEvent.
                XposedHelpers.findAndHookMethod("android.view.MotionEvent", lpparam.classLoader, "recycle", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mhparam) throws Throwable {
                        if (XposedHelpers.getBooleanField(mhparam.thisObject, "mRecycled")) {
                            XposedBridge.log("xbtnf: recycling a second time; block!");
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
                            XposedBridge.log("xbtnf: triggered Overview from AltTab. starting Overview!");
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

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!shouldDisableNavBarColorTweak) {
            XposedHelpers.findAndHookMethod("com.android.internal.policy.DecorView", null, "calculateNavigationBarColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam mhparam) throws Throwable {
                            int systemUiVisibility = (Integer) XposedHelpers.callMethod(
                                    mhparam.thisObject,
                                    "getSystemUiVisibility");
                            int windowFlags = ((Window) XposedHelpers.getObjectField(mhparam.thisObject, "mWindow")).getAttributes().flags;

                            // Force NavBar to be transparent when using Onyx Note
                            if (AndroidAppHelper.currentApplication().getPackageName().equals("com.onyx.android.note")) {
                                mhparam.setResult(
                                        Color.TRANSPARENT
                                );
                                XposedHelpers.callMethod(
                                        mhparam.thisObject,
                                        "setSystemUiVisibility",
                                        systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                                );
                            } else if (!shouldNavBarBeAlwaysDark && !shouldNavBarBeAlwaysLight) {
                                // BOOX OS automatically sets NavBar icon color to dark(SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                                //   when it detects a "light" NavBar.
                                // We redefine "light" NavBar here.

                                if (ModColorUtils.isRealLightColor((Integer) mhparam.getResult(), systemUiVisibility, windowFlags)) {
                                    XposedHelpers.callMethod(
                                            mhparam.thisObject,
                                            "setSystemUiVisibility",
                                            systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                                    );
                                }
                            } else if (shouldNavBarBeAlwaysLight) {
                                if (!ModColorUtils.isRealTransparentColor((Integer) mhparam.getResult(), systemUiVisibility, windowFlags)) {
                                    mhparam.setResult(
                                            Color.WHITE
                                    );
                                }
                                XposedHelpers.callMethod(
                                        mhparam.thisObject,
                                        "setSystemUiVisibility",
                                        systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                                );
                            } else {
                                mhparam.setResult(
                                        Color.BLACK
                                );
                                XposedHelpers.callMethod(
                                        mhparam.thisObject,
                                        "setSystemUiVisibility",
                                        systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
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
