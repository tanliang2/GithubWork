package com.tl.githubcompose // Use your base package

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Custom runner to set up the instrumented application class for Hilt tests.
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        // Force Hilt tests to use HiltTestApplication
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
} 