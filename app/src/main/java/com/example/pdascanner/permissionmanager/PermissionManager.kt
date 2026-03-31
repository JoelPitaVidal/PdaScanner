package com.example.pdascanner.permissionmanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class PermissionManager(private val activity: AppCompatActivity) {

    private val permissions = mutableListOf(Manifest.permission.CAMERA).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(activity.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}