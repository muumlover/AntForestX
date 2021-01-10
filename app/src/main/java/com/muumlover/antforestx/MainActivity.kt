package com.muumlover.antforestx

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream


class MainActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVHelper.init(this)
        initView()
        refreshView()
        testFinder()
        screenBaseInfo
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0

    /**
     * 获取屏幕基本信息
     */
    private val screenBaseInfo: Unit
        get() {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            mScreenWidth = metrics.widthPixels
            mScreenHeight = metrics.heightPixels
            mScreenDensity = metrics.densityDpi
        }

    private fun initView() {
        val inputStream = resources.openRawResource(R.raw.screen)
        imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream))

        val radius: SharedPreferences = getSharedPreferences("Radius", Context.MODE_PRIVATE)
        seekBarMin.progress = radius.getInt("minRadius", 60)
        seekBarMax.progress = radius.getInt("maxRadius", 70)

        swScreenShot.setOnClickListener(this)
        swAccessibilityService.setOnClickListener(this)
        btnOpenAntForest.setOnClickListener(this)
        button.setOnClickListener(this)
        seekBarMin.setOnSeekBarChangeListener(this)
        seekBarMax.setOnSeekBarChangeListener(this)
    }

    private fun refreshView() {
        if (AccessibilityHelper.hasServicePermission(
                applicationContext,
                AntForestService::class.java
            )
        ) {
            swAccessibilityService.isChecked = true
            if (AntForestService.isStart()) {
                swAccessibilityService.text = "无障碍服务正在运行"
            } else {
                swAccessibilityService.isChecked = false
                swAccessibilityService.text = "无障碍服务已经停止"
                startAntForest()
            }
        } else {
            swAccessibilityService.isChecked = false
        }
    }

    private var screenFile: String? = null
    private fun testFinder() {
        val screen = if (screenFile == null) {
            Utils.loadResource(this, R.raw.screen)
        } else {
            val dir: File = getDir("screen", Context.MODE_PRIVATE)
            val file = File(dir, "lastBitmap.png")
            val fis = FileInputStream(file)
            val lastBitmap = BitmapFactory.decodeStream(fis)
            fis.close()
            val screen = Mat()
            Utils.bitmapToMat(lastBitmap, screen)
            screen
        }
        Imgproc.cvtColor(screen, screen, Imgproc.COLOR_BGR2GRAY);
        val circles = Mat()
        Imgproc.HoughCircles(
            screen,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            10.0,
            100.0,
            30.0,
            seekBarMin.progress,
            seekBarMax.progress
        )
        txtBar.text = "[${seekBarMin.progress}, ${seekBarMax.progress}]"
        Log.d(TAG, "v1:" + seekBarMin.progress + "v2:" + seekBarMax.progress)

        for (i in 0 until circles.cols()) {
            val vCircle = circles[0, i]
            val center = Point(vCircle[0], vCircle[1])
            val radius = Math.round(vCircle[2]).toInt()

            // circle center
            Imgproc.circle(screen, center, 3, Scalar(0.0, 255.0, 0.0), -1, 8, 0)
            // circle outline
            Imgproc.circle(screen, center, radius, Scalar(0.0, 0.0, 255.0), 3, 8, 0)
        }
        val screenBitmap = Bitmap.createBitmap(
            screen.width(),
            screen.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(screen, screenBitmap)
        imageView.setImageBitmap(screenBitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK) {
                    //获得录屏权限，启动Service进行录制
                    val intent = Intent(this, CaptureService::class.java)
                    intent.putExtra("resultCode", resultCode)
                    intent.putExtra("resultData", data)
                    intent.putExtra("mScreenWidth", mScreenWidth)
                    intent.putExtra("mScreenHeight", mScreenHeight)
                    intent.putExtra("mScreenDensity", mScreenDensity)
                    startService(intent)
                    Log.d(TAG, "已经获取到屏幕截图权限")
                    //Toast.makeText(this, "成功开启服务", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "服务开启失败,无法截屏，退出", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            swAccessibilityService.id -> {
                if (swAccessibilityService.isChecked) {
                    try {
                        Log.d(TAG, "尝试跳转到无障碍设置页面")
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        Log.d(TAG, "无法跳转到无障碍设置页面")
                        e.printStackTrace()
                        Log.d(TAG, "跳转到系统设置页面")
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                } else {
                    stopAntForest()
                }
            }
            swScreenShot.id -> {
                if (swScreenShot.isChecked) {
                    startScreenRecord()
                } else {
                    stopScreenRecord()
                }
            }
            R.id.btnOpenAntForest -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("alipayqr://platformapi/startapp?saId=60000002")
                    )
                )
            }
            button.id -> {
                val dir: File = getDir("screen", Context.MODE_PRIVATE)
                val file = File(dir, "lastBitmap.png")
                if (file.exists()) {
                    screenFile = "lastBitmap"
                }
            }
        }
        refreshView()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when (seekBar?.id) {
            seekBarMax.id, seekBarMin.id -> {
                val radius: SharedPreferences = getSharedPreferences("Radius", Context.MODE_PRIVATE)
                val edit = radius.edit()
                edit.putInt("minRadius", seekBarMin.progress)
                edit.putInt("maxRadius", seekBarMax.progress)
                edit.apply()

                testFinder()
            }
        }
    }

    //start screen record.
    private fun startAntForest() {
        val service = Intent(this, AntForestService::class.java)
        this.startService(service)
    }

    //start screen record.
    private fun stopAntForest() {
        val service = Intent(this, AntForestService::class.java)
        stopService(service)
    }

    //start screen record
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startScreenRecord() {
        //Manages the retrieval of certain types of MediaProjection tokens.
        val mediaProjectionManager =
            this.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        //Returns an Intent that must passed to startActivityForResult() in order to start screen capture.
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        this.startActivityForResult(permissionIntent, REQUEST_MEDIA_PROJECTION)
    }

    //stop screen record.
    private fun stopScreenRecord() {
        val service = Intent(this, CaptureService::class.java)
        this.stopService(service)
    }

    companion object {
        private var REQUEST_MEDIA_PROJECTION = 1001
    }

}
