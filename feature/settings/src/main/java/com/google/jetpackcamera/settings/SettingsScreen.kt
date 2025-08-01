/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.settings

import android.Manifest
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.ui.AspectRatioSetting
import com.google.jetpackcamera.settings.ui.DarkModeSetting
import com.google.jetpackcamera.settings.ui.DefaultCameraFacing
import com.google.jetpackcamera.settings.ui.FlashModeSetting
import com.google.jetpackcamera.settings.ui.MaxVideoDurationSetting
import com.google.jetpackcamera.settings.ui.RecordingAudioSetting
import com.google.jetpackcamera.settings.ui.SETTINGS_TITLE
import com.google.jetpackcamera.settings.ui.SectionHeader
import com.google.jetpackcamera.settings.ui.SettingsPageHeader
import com.google.jetpackcamera.settings.ui.StabilizationSetting
import com.google.jetpackcamera.settings.ui.StreamConfigSetting
import com.google.jetpackcamera.settings.ui.TargetFpsSetting
import com.google.jetpackcamera.settings.ui.VersionInfo
import com.google.jetpackcamera.settings.ui.VideoQualitySetting
import com.google.jetpackcamera.settings.ui.theme.SettingsPreviewTheme

/**
 * Screen used for the Settings feature.
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    versionInfo: VersionInfoHolder,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()

    SettingsScreen(
        uiState = settingsUiState,
        versionInfo = versionInfo,
        onNavigateBack = onNavigateBack,
        setDefaultLensFacing = viewModel::setDefaultLensFacing,
        setFlashMode = viewModel::setFlashMode,
        setTargetFrameRate = viewModel::setTargetFrameRate,
        setAspectRatio = viewModel::setAspectRatio,
        setCaptureMode = viewModel::setStreamConfig,
        setAudio = viewModel::setVideoAudio,
        setStabilizationMode = viewModel::setStabilizationMode,
        setMaxVideoDuration = viewModel::setMaxVideoDuration,
        setDarkMode = viewModel::setDarkMode,
        setVideoQuality = viewModel::setVideoQuality
    )
    val permissionStates = rememberMultiplePermissionsState(
        permissions =
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )

    viewModel.setGrantedPermissions(permissionStates)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    versionInfo: VersionInfoHolder,
    onNavigateBack: () -> Unit = {},
    setDefaultLensFacing: (LensFacing) -> Unit = {},
    setFlashMode: (FlashMode) -> Unit = {},
    setTargetFrameRate: (Int) -> Unit = {},
    setAspectRatio: (AspectRatio) -> Unit = {},
    setCaptureMode: (StreamConfig) -> Unit = {},
    setStabilizationMode: (StabilizationMode) -> Unit = {},
    setAudio: (Boolean) -> Unit = {},
    setMaxVideoDuration: (Long) -> Unit = {},
    setDarkMode: (DarkMode) -> Unit = {},
    setVideoQuality: (VideoQuality) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsPageHeader(
                modifier = Modifier.testTag(SETTINGS_TITLE),
                title = stringResource(id = R.string.settings_title),
                navBack = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            if (uiState is SettingsUiState.Enabled) {
                SettingsList(
                    uiState = uiState,
                    versionInfo = versionInfo,
                    setDefaultLensFacing = setDefaultLensFacing,
                    setFlashMode = setFlashMode,
                    setTargetFrameRate = setTargetFrameRate,
                    setAspectRatio = setAspectRatio,
                    setCaptureMode = setCaptureMode,
                    setStabilizationMode = setStabilizationMode,
                    setAudio = setAudio,
                    setMaxVideoDuration = setMaxVideoDuration,
                    setDarkMode = setDarkMode,
                    setVideoQuality = setVideoQuality
                )
            }
        }
    }
}

@Composable
fun SettingsList(
    uiState: SettingsUiState.Enabled,
    versionInfo: VersionInfoHolder,
    setDefaultLensFacing: (LensFacing) -> Unit = {},
    setFlashMode: (FlashMode) -> Unit = {},
    setTargetFrameRate: (Int) -> Unit = {},
    setAspectRatio: (AspectRatio) -> Unit = {},
    setCaptureMode: (StreamConfig) -> Unit = {},
    setAudio: (Boolean) -> Unit = {},
    setStabilizationMode: (StabilizationMode) -> Unit = {},
    setVideoQuality: (VideoQuality) -> Unit = {},
    setMaxVideoDuration: (Long) -> Unit = {},
    setDarkMode: (DarkMode) -> Unit = {}
) {
    SectionHeader(title = stringResource(id = R.string.section_title_camera_settings))

    DefaultCameraFacing(
        lensUiState = uiState.lensFlipUiState,
        setDefaultLensFacing = setDefaultLensFacing
    )

    FlashModeSetting(
        flashUiState = uiState.flashUiState,
        setFlashMode = setFlashMode
    )

    TargetFpsSetting(
        fpsUiState = uiState.fpsUiState,
        setTargetFps = setTargetFrameRate
    )

    AspectRatioSetting(
        aspectRatioUiState = uiState.aspectRatioUiState,
        setAspectRatio = setAspectRatio
    )

    StreamConfigSetting(
        streamConfigUiState = uiState.streamConfigUiState,
        setStreamConfig = setCaptureMode
    )

    SectionHeader(title = stringResource(R.string.section_title_recording_settings))

    RecordingAudioSetting(
        audioUiState = uiState.audioUiState,
        setDefaultAudio = setAudio
    )

    MaxVideoDurationSetting(
        maxVideoDurationUiState = uiState.maxVideoDurationUiState,
        setMaxDuration = setMaxVideoDuration
    )

    StabilizationSetting(
        stabilizationUiState = uiState.stabilizationUiState,
        setStabilizationMode = setStabilizationMode
    )

    VideoQualitySetting(
        videQualityUiState = uiState.videoQualityUiState,
        setVideoQuality = setVideoQuality
    )

    SectionHeader(title = stringResource(id = R.string.section_title_app_settings))

    DarkModeSetting(
        darkModeUiState = uiState.darkModeUiState,
        setDarkMode = setDarkMode
    )

    SectionHeader(title = stringResource(id = R.string.section_title_software_info))

    VersionInfo(
        versionName = versionInfo.versionName,
        buildType = versionInfo.buildType
    )
}

// will allow you to open stabilization popup or give disabled rationale

data class VersionInfoHolder(val versionName: String, val buildType: String)

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SettingsScreen() {
    SettingsPreviewTheme {
        SettingsScreen(
            uiState = TYPICAL_SETTINGS_UISTATE,
            versionInfo = VersionInfoHolder(
                versionName = "1.0.0",
                buildType = "release"
            )
        )
    }
}
