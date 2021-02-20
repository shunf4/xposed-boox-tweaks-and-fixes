## XBTNF

Various Fixs and Tweaks to BOOX as a Xposed module.

Only tested on Max3 with ROM 2020-11-04_07_07_3.0_9aed398. Using this module could cause harm to your device. No warranty is assured!

### Usage

0. Backup all data on your device
1. Disable App List mode in EdXposed Manager (Maybe it's optional?)
2. Install this module
3. Go to EdXposed Manager - Modules and press "refresh" button (since BOOX OS does not allow EdXposed Manager to automatically take action after an app is installed/upgraded; you can enable "EdXposed Manager" module to make this mechanism work again)
4. Enable this module in EdXposed Manager
5. Create files in `/data/local` with root permission if you want to switch on/off some tweaks;
    See below
6. Reboot
7. Enjoy

### Features

**You need to install and enable the [Show Navigation Bar](https://github.com/shunf4/xposed-boox-tweaks-and-fixes/files/5919306/Show_navigation_bar-v06.zip) Magisk module to enjoy (the navigation bar and) the tweaks in this Xposed module.**

Also, you are advised to install [SystemUI Tuner Redesigned](https://play.google.com/store/apps/details?id=com.zacharee1.systemuituner&hl=en_US) to tune more things (per-app immersive mode etc.)

- Fix crash when tapping navigation bar buttons after enabling navigation bar  
    (Default is enabled; to disable, create the file `/data/local/xbtnf_disable_navbar_button_fix`)

- Fix navigation bar color (make it transparent when in "Onyx Note" app; fix NavBar button colors in some apps)
    (Default is enabled; to disable, create the file `/data/local/xbtnf_disable_navbar_color_tweak`;
        if you want an always-dark NavBar, create the file `/data/local/xbtnf_navbar_dark`;
        if you want an always-light NavBar, create the file `/data/local/xbtnf_navbar_light`.)

- Fix the form of NavBar, switching the original ["SwipeUpUI" NavBar](https://www.androidpolice.com/2018/05/14/android-ps-new-overview-app-switcher-part-launcher/) to a normal form with back/home/recent buttons  
    (Default is enabled; to disable, create the file `/data/local/xbtnf_disable_navbar_form_fix`)

- Change the layout of NavBar buttons with a layout string (Please decompile `SystemUI.apk` from the ROM and see in `NavigationBarInflaterView` class how your BOOX handles the string and generate the layout. Here's the [source code](https://github.com/aosp-mirror/platform_frameworks_base/blob/pie-release/packages/SystemUI/src/com/android/systemui/statusbar/phone/NavigationBarInflaterView.java) just for reference)  
    (Default is enabled, changing the layout to `left[.5W],back[1WC];space[1W],home[1WC],space[1W];recent[1WC],right[.5W]`; to change the layout string, create the file `/data/local/xbtnf_navbar_layout` and write the string into it. If you write nothing more than whitespaces, the fix is disabled, falling back to the default string `back,home,left;space;right,recent` which only shows a single "back" button for unknown reason)

- Make `Alt+Tab` able to bring up Overview (Recent Task Switcher)  
    (Default is enabled; to disable, create the file `/data/local/xbtnf_disable_alt_tab_overview_tweak`)

- Allow changing the E-Ink optimization configuration for system (whitelisted) apps (Be careful!)
    (Default is enabled; to disable, create the file `/data/local/xbtnf_disable_all_apps_eink_optimizable_tweak`)

