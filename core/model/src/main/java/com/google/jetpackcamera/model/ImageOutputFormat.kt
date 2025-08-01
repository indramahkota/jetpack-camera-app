/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.model

import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto

val DEFAULT_HDR_IMAGE_OUTPUT = ImageOutputFormat.JPEG_ULTRA_HDR

enum class ImageOutputFormat {
    JPEG,
    JPEG_ULTRA_HDR;

    companion object {

        /** returns the DynamicRangeType enum equivalent of a provided DynamicRangeTypeProto */
        fun fromProto(imageOutputFormatProto: ImageOutputFormatProto): ImageOutputFormat {
            return when (imageOutputFormatProto) {
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR -> JPEG_ULTRA_HDR

                // Treat unrecognized as JPEG as a fallback
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
                ImageOutputFormatProto.UNRECOGNIZED -> JPEG
            }
        }

        fun ImageOutputFormat.toProto(): ImageOutputFormatProto {
            return when (this) {
                JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
                JPEG_ULTRA_HDR -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
            }
        }
    }
}
