/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package com.posed.lspd.core;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.android.internal.os.ZygoteInit;
import com.debin.android.fun.XposedBridge;
import com.debin.android.fun.XposedHelpers;
import com.debin.android.fun.XposedInit;
import com.posed.lspd.deopt.PrebuiltMethodsDeopter;
import com.posed.lspd.hooker.CrashDumpHooker;
import com.posed.lspd.hooker.HandleSystemServerProcessHooker;
import com.posed.lspd.hooker.LoadedApkCtorHooker;
import com.posed.lspd.hooker.OpenDexFileHooker;
import com.posed.lspd.service.ILSPApplicationService;
import com.posed.lspd.util.Utils;

import dalvik.system.DexFile;

public class Startup {
    @SuppressWarnings("deprecation")
    private static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        XposedHelpers.findAndHookMethod(Thread.class, "dispatchUncaughtException",
                Throwable.class, new CrashDumpHooker());
        if (isSystem) {
            XposedBridge.hookAllMethods(ZygoteInit.class,
                    "handleSystemServerProcess", new HandleSystemServerProcessHooker());
        } else {
            var hooker = new OpenDexFileHooker();
            XposedBridge.hookAllMethods(DexFile.class, "openDexFile", hooker);
            XposedBridge.hookAllMethods(DexFile.class, "openInMemoryDexFile", hooker);
            XposedBridge.hookAllMethods(DexFile.class, "openInMemoryDexFiles", hooker);
        }
        XposedHelpers.findAndHookConstructor(LoadedApk.class,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class,
                new LoadedApkCtorHooker());
    }

    public static void bootstrapXposed() {
        // Initialize the Xposed framework
        try {
            startBootstrapHook(XposedInit.startsSystemServer);
            XposedInit.loadModules();
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
        }
    }

    public static void initXposed(boolean isSystem, String processName, ILSPApplicationService service) {
        // init logger
        ApplicationServiceClient.Init(service, processName);
        XposedBridge.initXResources();
        XposedInit.startsSystemServer = isSystem;
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
    }
}
