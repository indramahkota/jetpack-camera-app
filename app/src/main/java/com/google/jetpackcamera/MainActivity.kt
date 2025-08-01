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
package com.google.jetpackcamera

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tracing.Trace
import com.google.jetpackcamera.MainActivityUiState.Loading
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.core.common.traceFirstFrameMainActivity
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.JcaApp
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

/**
 * Activity for the JetpackCameraApp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.M)
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var uiState: MainActivityUiState by mutableStateOf(Loading)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }

        var firstFrameComplete: CompletableDeferred<Unit>? = null
        if (Trace.isEnabled()) {
            firstFrameComplete = CompletableDeferred()
            // start trace between app starting and the earliest possible completed capture
            lifecycleScope.launch {
                traceFirstFrameMainActivity(cookie = 0) {
                    firstFrameComplete.await()
                }
            }
        }

        setContent {
            when (uiState) {
                Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(50.dp))
                        Text(text = stringResource(R.string.jca_loading), color = Color.White)
                    }
                }

                is Success -> {
                    // TODO(kimblebee@): add app setting to enable/disable dynamic color
                    JetpackCameraTheme(
                        darkTheme = isInDarkMode(uiState = uiState),
                        dynamicColor = false
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    testTagsAsResourceId = true
                                },
                            color = MaterialTheme.colorScheme.background
                        ) {
                            JcaApp(
                                externalCaptureMode = getPreviewMode(),
                                debugSettings = debugSettings,
                                openAppSettings = ::openAppSettings,
                                onRequestWindowColorMode = { colorMode ->
                                    // Window color mode APIs require API level 26+
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Log.d(
                                            TAG,
                                            "Setting window color mode to:" +
                                                " ${colorMode.toColorModeString()}"
                                        )
                                        window?.colorMode = colorMode
                                    }
                                },
                                onFirstFrameCaptureCompleted = {
                                    firstFrameComplete?.complete(Unit)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private val debugSettings: DebugSettings
        get() = DebugSettings(
            isDebugModeEnabled = intent?.getBooleanExtra(KEY_DEBUG_MODE, false) ?: false,
            singleLensMode = intent?.getStringExtra(KEY_DEBUG_SINGLE_LENS_MODE)
                ?.let {
                    when (it.lowercase()) {
                        "back" -> LensFacing.BACK
                        "front" -> LensFacing.FRONT
                        else -> {
                            Log.e(
                                TAG,
                                "Invalid debug single lens mode argument: \"$it\". Valid values are \"FRONT\" or \"BACK\""
                            )
                            null
                        }
                    }
                }
        )

    private fun getStandardMode(): ExternalCaptureMode.StandardMode {
        return ExternalCaptureMode.StandardMode { event ->
            if (event is ExternalCaptureMode.ImageCaptureEvent.ImageSaved) {
                @Suppress("DEPRECATION")
                val intent = Intent(Camera.ACTION_NEW_PICTURE)
                intent.setData(event.savedUri)
                sendBroadcast(intent)
            }
        }
    }

    private fun getExternalCaptureUri(): Uri? {
        return IntentCompat.getParcelableExtra(
            intent,
            MediaStore.EXTRA_OUTPUT,
            Uri::class.java
        ) ?: intent?.clipData?.getItemAt(0)?.uri
    }

    private fun getMultipleExternalCaptureUri(): List<Uri>? {
        val stringUris = intent.getStringArrayListExtra(MediaStore.EXTRA_OUTPUT)
        if (stringUris.isNullOrEmpty()) {
            return null
        } else {
            val result = mutableListOf<Uri>()
            for (string in stringUris) {
                result.add(Uri.parse(string))
            }
            return result
        }
    }

    private fun getPreviewMode(): ExternalCaptureMode {
        return intent?.action?.let { action ->
            when (action) {
                MediaStore.ACTION_IMAGE_CAPTURE ->
                    ExternalCaptureMode.ExternalImageCaptureMode(getExternalCaptureUri()) { event ->
                        Log.d(TAG, "onImageCapture, event: $event")
                        if (event is ExternalCaptureMode.ImageCaptureEvent.ImageSaved) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(MediaStore.EXTRA_OUTPUT, event.savedUri)
                            setResult(RESULT_OK, resultIntent)
                            Log.d(TAG, "onImageCapture, finish()")
                            finish()
                        }
                    }

                MediaStore.ACTION_VIDEO_CAPTURE ->
                    ExternalCaptureMode.ExternalVideoCaptureMode(getExternalCaptureUri()) { event ->
                        Log.d(TAG, "onVideoCapture, event: $event")
                        if (event is ExternalCaptureMode.VideoCaptureEvent.VideoSaved) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(MediaStore.EXTRA_OUTPUT, event.savedUri)
                            setResult(RESULT_OK, resultIntent)
                            Log.d(TAG, "onVideoCapture, finish()")
                            finish()
                        }
                    }

                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA -> {
                    val uriList: List<Uri>? = getMultipleExternalCaptureUri()
                    val pictureTakenUriList: ArrayList<String?> = arrayListOf()
                    ExternalCaptureMode.ExternalMultipleImageCaptureMode(
                        uriList
                    ) { event: ExternalCaptureMode.ImageCaptureEvent, uriIndex: Int ->
                        Log.d(TAG, "onMultipleImageCapture, event: $event")
                        if (uriList == null) {
                            when (event) {
                                is ExternalCaptureMode.ImageCaptureEvent.ImageSaved ->
                                    pictureTakenUriList.add(event.savedUri.toString())
                                is ExternalCaptureMode.ImageCaptureEvent.ImageCaptureError ->
                                    pictureTakenUriList.add(event.exception.toString())
                            }
                            val resultIntent = Intent()
                            resultIntent.putStringArrayListExtra(
                                MediaStore.EXTRA_OUTPUT,
                                pictureTakenUriList
                            )
                            setResult(RESULT_OK, resultIntent)
                        } else if (uriIndex == uriList.size - 1) {
                            setResult(RESULT_OK, Intent())
                            Log.d(TAG, "onMultipleImageCapture, finish()")
                            finish()
                        }
                    }
                }

                else -> {
                    Log.w(TAG, "Ignoring external intent with unknown action.")
                    getStandardMode()
                }
            }
        } ?: getStandardMode()
    }

    companion object {
        private const val KEY_DEBUG_MODE = "KEY_DEBUG_MODE"
        const val KEY_DEBUG_SINGLE_LENS_MODE = "KEY_DEBUG_SINGLE_LENS_MODE"
    }
}

/**
 * Determines whether the Theme should be in dark, light, or follow system theme
 */
@Composable
private fun isInDarkMode(uiState: MainActivityUiState): Boolean = when (uiState) {
    Loading -> isSystemInDarkTheme()
    is Success -> when (uiState.cameraAppSettings.darkMode) {
        DarkMode.DARK -> true
        DarkMode.LIGHT -> false
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Int.toColorModeString(): String {
    return when (this) {
        ActivityInfo.COLOR_MODE_DEFAULT -> "COLOR_MODE_DEFAULT"
        ActivityInfo.COLOR_MODE_HDR -> "COLOR_MODE_HDR"
        ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT -> "COLOR_MODE_WIDE_COLOR_GAMUT"
        else -> "<Unknown>"
    }
}

/**
 * Open the app settings when necessary. I.e. to enable permissions that have been denied by a user
 */
private fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
