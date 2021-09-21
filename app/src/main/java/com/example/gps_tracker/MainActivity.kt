package com.example.gps_tracker


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.example.gps_tracker.BuildConfig
import com.example.gps_tracker.R

import com.example.gps_tracker.services.LocationMonitoringService

class MainActivity:AppCompatActivity() {
    private var mAlreadyStartedService = false
    private var mMsgView:TextView? = null

     val isGooglePlayServicesAvailable:Boolean
    get() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS)
        {
            if (googleApiAvailability.isUserResolvableError(status))
            {
                googleApiAvailability.getErrorDialog(this, status, 2404).show()
            }
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMsgView = findViewById(R.id.msgView) as TextView


        LocalBroadcastManager.getInstance(this).registerReceiver(
            object:BroadcastReceiver() {
                override fun onReceive(context:Context, intent:Intent) {
                    val latitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE)
                    val longitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE)

                    if (latitude != null && longitude != null)
                    {
                        mMsgView!!.text = getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude
                    }
                }
            }, IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
            )
    }

    public override fun onResume() {
        super.onResume()
        startStep1()
    }


    private fun startStep1() {

        if (isGooglePlayServicesAvailable)
        {

            startStep2(null)
        }
        else
        {
            Toast.makeText(applicationContext, R.string.no_google_playservice_available, Toast.LENGTH_LONG).show()
        }
    }



    private fun startStep2(dialog:DialogInterface?):Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected)
        {
            promptInternetConnect()
            return false
        }


        if (dialog != null)
        {
            dialog!!.dismiss()
        }



        if (checkPermissions())
        {
            startStep3()
        }
        else
        {
            requestPermissions()
        }
        return true
    }


    private fun promptInternetConnect() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(R.string.title_alert_no_intenet)
        builder.setMessage(R.string.msg_alert_no_internet)

        val positiveText = getString(R.string.btn_label_refresh)
        builder.setPositiveButton(positiveText) { dialog, which ->

            if (startStep2(dialog)) {

                if (checkPermissions()) {

                    startStep3()
                } else if (!checkPermissions()) {
                    requestPermissions()
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }


    private fun startStep3() {

        if (!mAlreadyStartedService && mMsgView != null)
        {
            mMsgView!!.setText(R.string.msg_location_service_started)

            val intent = Intent(this, LocationMonitoringService::class.java)
            startService(intent)

            mAlreadyStartedService = true
        }
    }



    private fun checkPermissions():Boolean {
        val permissionState1 = ActivityCompat.checkSelfPermission(this,
        android.Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionState2 = ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION)

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
        android.Manifest.permission.ACCESS_FINE_LOCATION)

        val shouldProvideRationale2 = ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.ACCESS_COARSE_LOCATION)


        if (shouldProvideRationale || shouldProvideRationale2)
        {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale,
            android.R.string.ok, View.OnClickListener {

                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        }
        else
        {
            Log.i(TAG, "Requesting permission")

            ActivityCompat.requestPermissions(this@MainActivity,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }



    private fun showSnackbar(mainTextStringId:Int, actionStringId:Int, listener:View.OnClickListener) {
        Snackbar.make(
            findViewById(android.R.id.content), getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(getString(actionStringId), listener).show()
    }


    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults:IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE)
        {
            if (grantResults.size <= 0)
            {

                Log.i(TAG, "User interaction was cancelled.")
            }
            else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.i(TAG, "Permission granted, updates requested, starting location updates")
                startStep3()
            }
            else
            {

                showSnackbar(R.string.permission_denied_explanation,
                    R.string.settings, object:View.OnClickListener {
                    override fun onClick(view:View) {
                         // Build intent that displays the App settings screen.
                                                        val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                })
            }
        }
    }


    public override fun onDestroy() {

        stopService(Intent(this, LocationMonitoringService::class.java))
        mAlreadyStartedService = false

        super.onDestroy()
    }

    companion object {
        private val TAG = MainActivity::class.java!!.simpleName


        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }

}

