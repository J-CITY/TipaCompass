package com.lesson.dg.tipacompass

import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.abs






class MainActivity : AppCompatActivity(), SensorEventListener {

    private var currentDegree = 0f
    private lateinit var sensorManager: SensorManager
    var accSensor: Sensor? = null
    var magnetSensor: Sensor? = null

    lateinit var ad: InterstitialAd

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val xDelta: Int = 0
    private val yDelta: Int = 0

    private val size = Point()

    private val ON_TABLE = "On the table"
    private val NOT_ON_TABLE = "Not on the table"

    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    var start = 0L
    var delta = 600

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713")
        ad = InterstitialAd(this)
        ad.adUnitId = "ca-app-pub-3940256099942544/8691691433"
        ad.loadAd(AdRequest.Builder().addTestDevice("33293CAFEB24FBEA2DB6F6DF355BD150").build())

        //if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        //    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //}
        staticPoint.visibility = View.INVISIBLE
        dynamicPoint.visibility = View.INVISIBLE

        val display = getWindowManager().getDefaultDisplay()

        display.getSize(size)
        staticPoint.x = 0F
        staticPoint.y = 0F

        dynamicPoint.x = 0F
        dynamicPoint.y = 0F

        textView.text = NOT_ON_TABLE

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)


        start = System.currentTimeMillis()
    }

    fun onClickBtn(v: View) {
        //println("CLICK")
        if (ad.isLoaded()) {

            ad.show()
            staticPoint.visibility = View.VISIBLE
            dynamicPoint.visibility = View.VISIBLE
            fab.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accSensor)
        sensorManager.unregisterListener(this, magnetSensor)
    }

    var gravity: FloatArray? = null
    var geoMagnetic: FloatArray? = null
    var azimut: Float = 0.toFloat()
    var pitch: Float = 0.toFloat()
    var roll: Float = 0.toFloat()

    override fun onSensorChanged(event: SensorEvent?) {

        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event!!.values.clone()

            dynamicPoint.x = (relativeLayout.width / 2-dynamicPoint.width/2)+event!!.values[0]*10
            dynamicPoint.y = (relativeLayout.height / 2-dynamicPoint.height/2)+event!!.values[1]*10
            println(""+dynamicPoint.x.toString() + " " + dynamicPoint.y.toString())


            if (abs(event!!.values[0]) < 1.0f &&  abs(event!!.values[1]) < 1.0f
                && abs(event!!.values[2]) > 3.0f) {
                textView.text = ON_TABLE
            } else {
                textView.text = NOT_ON_TABLE
            }
        }
        if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
            geoMagnetic = event!!.values.clone()

        if (gravity != null && geoMagnetic != null) {

            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, gravity, geoMagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                azimut = 57.29578f * orientation[0]
                pitch = 57.29578f * orientation[1]
                roll = 57.29578f * orientation[2]

                val dist = abs((1.4f * Math.tan(pitch * Math.PI / 180)).toFloat())
                var ra = RotateAnimation(
                        currentDegree,
                        -azimut,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f)

                ra.setDuration(210)
                ra.setFillAfter(true)
                if (abs(abs(currentDegree) - abs(azimut)) > 0.2) {
                    imageCompass.startAnimation(ra)
                    currentDegree = -azimut
                    println("AZIMUT --> " +abs(abs(currentDegree) - abs(azimut)).toString() +" " +(abs(abs(currentDegree) - abs(azimut)) > 0.1).toString())
                }
                println("AZIMUT --> " +abs(abs(currentDegree) - abs(azimut)).toString() +" " +(abs(abs(currentDegree) - abs(azimut)) > 0.1).toString())


                superLog(-azimut)
                //Log.d("log", "orientation values: $azimut / $pitch / $roll dist = $dist")
            }
        }
    }

    fun superLog(logme: Float) {

        var t = System.currentTimeMillis();

        if (abs(start - t) > delta) {
            start = t

            var log = ""
            if ((logme > 0 && logme <= 45) || (logme <= 360 && logme > 270)) {
                log = "SOUTH"
            } else if (logme > 45 && logme <= 135) {
                log = "EAST"
            } else if (logme > 135 && logme <= 225) {
                log = "NORTH"
            } else {
                log = "WEST"
            }

            val bundle = Bundle()
            bundle.putString("image_name", "DIR")
            bundle.putString("full_text", log)
            mFirebaseAnalytics!!.logEvent("share_image", bundle)

        }
    }

}
