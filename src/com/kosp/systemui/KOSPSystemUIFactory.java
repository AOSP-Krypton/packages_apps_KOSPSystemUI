/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2021-2023 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kosp.systemui;

import android.content.Context;

import com.kosp.systemui.dagger.DaggerKOSPGlobalRootComponent;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;
import com.kosp.systemui.dagger.KOSPSysUIComponent;

import java.util.concurrent.ExecutionException;

public class KOSPSystemUIFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerKOSPGlobalRootComponent.builder()
                .context(context)
                .build();
    }

    @Override
    public void init(Context context, boolean fromTest)
            throws ExecutionException, InterruptedException {
        super.init(context, fromTest);
        if (shouldInitializeComponents()) {
            final KOSPSysUIComponent component = (KOSPSysUIComponent) getSysUIComponent();
            component.createKeyguardSmartspaceController();
            component.getLiveDisplayInitReceiver().register();
        }
    }
}