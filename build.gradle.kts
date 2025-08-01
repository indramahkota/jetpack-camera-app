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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
        alias(libs.plugins.compose.compiler) apply false
}

tasks.register<Copy>("installGitHooks") {
    println("Installing git hooks")
    from(rootProject.rootDir.resolve("hooks/pre-commit"))
    into(rootProject.rootDir.resolve(".git/hooks"))
    filePermissions {
        unix("0775")
    }
}

gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        if (task != tasks["installGitHooks"]) {
            task.dependsOn(tasks["installGitHooks"])
        }
    }
}

// Task to print all the module paths in the project e.g. :core:data
// Used by module graph generator script
tasks.register("printModulePaths") {
    subprojects {
        if (subprojects.size == 0) {
            println(this.path)
        }
    }
}