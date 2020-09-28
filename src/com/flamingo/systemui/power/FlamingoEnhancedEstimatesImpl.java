/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2022 FlamingoOS Project
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

package com.flamingo.systemui.power;

import static com.android.internal.util.flamingo.FlamingoUtils.isPackageInstalled;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;

import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.util.settings.GlobalSettings;

import java.time.Duration;

import javax.inject.Inject;

@SysUISingleton
public final class FlamingoEnhancedEstimatesImpl implements EnhancedEstimates {

    private static final String TAG = "FlamingoEnhancedEstimatesImpl";

    private static final Estimate EMPTY_ESTIMATE = new Estimate(-1L, false, -1L);

    private static final Duration DAY = Duration.ofDays(1L);
    private static final long HOUR = Duration.ofHours(1L).toMillis();
    private static final long THREE_HOURS = Duration.ofHours(3L).toMillis();
    private static final long FIFTEEN_MINUTES = Duration.ofMinutes(15L).toMillis();

    private final Context mContext;
    private final GlobalSettings mGlobalSettings;
    private final KeyValueListParser mParser;

    @Inject
    public FlamingoEnhancedEstimatesImpl(
        Context context,
        GlobalSettings globalSettings
    ) {
        mContext = context;
        mGlobalSettings = globalSettings;
        mParser = new KeyValueListParser(',');
    }

    @Override
    public boolean isHybridNotificationEnabled() {
        final boolean isTurboInstalled = isPackageInstalled(
            mContext,
            "com.google.android.apps.turbo",
            false /* ignoreState */
        );
        if (!isTurboInstalled) return false;
        updateFlags();
        return mParser.getBoolean("hybrid_enabled", true);
    }

    @Override
    public Estimate getEstimate() {
        final Uri build = new Uri.Builder()
            .scheme("content")
            .authority("com.google.android.apps.turbo.estimated_time_remaining")
            .appendPath("time_remaining")
            .build();
        try (final Cursor query = mContext.getContentResolver().query(build, null, null, null, null)) {
            if (query == null) return EMPTY_ESTIMATE;
            try {
                if (query.moveToFirst()) {
                    long timeRemaining = -1L;
                    final int usageColumnIndex = query.getColumnIndex("is_based_on_usage");
                    final boolean isBasedOnUsage = usageColumnIndex != -1 && query.getInt(usageColumnIndex) != 0;
                    final int batteryLifecolumnIndex = query.getColumnIndex("average_battery_life");
                    if (batteryLifecolumnIndex != -1) {
                        final long averageBatteryLife = query.getLong(batteryLifecolumnIndex);
                        if (averageBatteryLife != -1L) {
                            final long duration = Duration.ofMillis(averageBatteryLife).compareTo(DAY) >= 0
                                ? HOUR : FIFTEEN_MINUTES;
                            timeRemaining = PowerUtil.roundTimeToNearestThreshold(averageBatteryLife, duration);
                        }
                    }
                    return new Estimate(
                        query.getLong(query.getColumnIndex("battery_estimate")),
                        isBasedOnUsage,
                        timeRemaining
                    );
                }
            } catch (Exception ex) {
                // Catch and release
            }
        } catch (Exception e) {
            Log.e(TAG, "Something went wrong when getting an estimate from Turbo", e);
        }
        return EMPTY_ESTIMATE;
    }

    @Override
    public long getLowWarningThreshold() {
        updateFlags();
        return mParser.getLong("low_threshold", THREE_HOURS);
    }

    @Override
    public long getSevereWarningThreshold() {
        updateFlags();
        return mParser.getLong("severe_threshold", HOUR);
    }

    @Override
    public boolean getLowWarningEnabled() {
        updateFlags();
        return mParser.getBoolean("low_warning_enabled", false);
    }

    private void updateFlags() {
        final String string = mGlobalSettings.getString("hybrid_sysui_battery_warning_flags");
        try {
            mParser.setString(string);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Bad hybrid sysui warning flags");
        }
    }
}
