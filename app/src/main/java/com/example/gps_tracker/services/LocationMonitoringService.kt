package com.example.gps_tracker.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.support.annotation.Nullable
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.example.gps_tracker.R

import com.example.gps_tracker.settings.Constants

import java.util.List;

class LocationMonitoringService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    internal lateinit  var mLocationClient: GoogleApiClient
    internal var mLocationRequest = LocationRequest()
    internal var mLastLocation: Location? = null
    internal lateinit  var mFusedLocationClient: FusedLocationProviderClient

    internal var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.size > 0) {

                val location = locationList[locationList.size - 1]
                Log.i("MapsActivity", "Location: " + location.latitude + " " + location.longitude)
                mLastLocation = location
                if (mLastLocation != null) {
                    Log.d(TAG, "== location != null")

                    //Send result to activities
                    sendMessageToUI(mLastLocation!!.latitude.toString(), mLastLocation!!.longitude.toString())
                }
            }
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = getString(R.string.channel_name)
        val description = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = description
        val notificationManager = getSystemService(NotificationManager::class.java)

            notificationManager!!.createNotificationChannel(channel)
        }
        return channelId
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ENTRA onCreate")
        val channelId = createNotificationChannel("my_service", "My Background Service")

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking")
            .setContentText("Traking your current position")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        startForeground(Constants.ONGOING_NOTIFICATION_ID, notification);
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {


        mLocationClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        mLocationRequest.setInterval(Constants.LOCATION_INTERVAL)
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL)


        val priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        mLocationRequest.priority = priority
        mLocationClient.connect()

        Log.d(TAG, "mFusedLocationClient")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onConnected(dataBundle: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "== Error On onConnected() Permission not granted")


            return
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())


        Log.d(TAG, "Connected to Google API")
    }


    override fun onConnectionSuspended(i: Int) {
        Log.d(TAG, "Connection suspended")
    }

    private fun sendMessageToUI(lat: String, lng: String) {

        Log.d(TAG, "Sending info...")

        val intent = Intent(ACTION_LOCATION_BROADCAST)
        intent.putExtra(EXTRA_LATITUDE, lat)
        intent.putExtra(EXTRA_LONGITUDE, lng)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "Failed to connect to Google API")

    }

    companion object {

        private val TAG = LocationMonitoringService::class.java!!.getSimpleName()

        val ACTION_LOCATION_BROADCAST = LocationMonitoringService::class.java!!.getName() + "LocationBroadcast"
        val EXTRA_LATITUDE = "extra_latitude"
        val EXTRA_LONGITUDE = "extra_longitude"
    }
}
