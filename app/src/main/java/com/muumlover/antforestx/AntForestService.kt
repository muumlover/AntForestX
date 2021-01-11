package com.muumlover.antforestx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.opencv.android.Utils
import org.opencv.core.Core.minMaxLoc
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.matchTemplate
import java.io.File
import java.io.FileOutputStream
import java.util.*


fun findNodeById(root: AccessibilityNodeInfo, childName: String): AccessibilityNodeInfo? {
    for (i: Int in 0 until root.childCount) {
        val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
        if (child.viewIdResourceName == childName) return child
        if (child.childCount > 0) {
            val node: AccessibilityNodeInfo? = findNodeById(child, childName)
            if (node != null) return node
        }
    }
    return null
}

val AccessibilityNodeInfo.textOrDesc: CharSequence
    get() {
        var childText = this.contentDescription
        if (childText == null) childText = this.text
        if (childText == null) return ""
        return childText
    }

class AntForestService : AccessibilityService() {
    private val TAG = javaClass.name
    private var titleNow: CharSequence = ""
    private var isClicking = false
    private var clickFinish = false
    private var clickNodeList = ArrayList<AccessibilityNodeInfo>()

    companion object {
        var mForestService: AntForestService? = null

        /**
         * 辅助功能是否启动
         */
        fun isStart(): Boolean {
            return mForestService != null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(TAG, "无障碍服务启动成功")
        mForestService = this
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
        mForestService = null
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "无障碍服务已关闭")
        mForestService = null
    }


    private val nameList = listOf("蚂蚁森林", "好友排行榜")
    private val descList = listOf("地图", "成就", "通知", "背包", "任务", "攻略", "发消息", "弹幕", "浇水")

    fun test(image: Mat, templ: Mat, method: Int) {
        val result = Mat()
        matchTemplate(image, templ, result, method)
        val location = minMaxLoc(result)
        Log.d(TAG, "minLoc ${location.minLoc} ${location.minVal}")
        Log.d(TAG, "maxLoc ${location.maxLoc} ${location.maxVal}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
//        Log.v(TAG, "${eventTypeToString(event.eventType)} :: ${event.className}")
        val node: AccessibilityNodeInfo = event.source ?: return
        node.refresh()
        val h5TvTitle = findNodeById(node, "com.alipay.mobile.nebula:id/h5_tv_title") ?: return
        h5TvTitle.refresh()
        if (h5TvTitle.textOrDesc == "") return
        if (this.titleNow == h5TvTitle.textOrDesc) if (this.isClicking || this.clickFinish) return
        else this.clickNodeList.clear()
        this.titleNow = h5TvTitle.textOrDesc
        this.clickFinish = false
        // H5标题不是蚂蚁森林就忽略
        if (!h5TvTitle.textOrDesc.endsWith("蚂蚁森林")) return
        Log.d(TAG, "当前页面为：<${h5TvTitle.textOrDesc}>")

        val radius: SharedPreferences = getSharedPreferences("Radius", Context.MODE_PRIVATE)
        val minRadius: Int = radius.getInt("minRadius", 60)
        val maxRadius: Int = radius.getInt("maxRadius", 70)

        val screen = getScreen()
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
            minRadius,
            maxRadius
        )
        Thread.sleep(400)
        for (i in 0 until circles.cols()) {
            val vCircle = circles[0, i]
            val center = Point(vCircle[0], vCircle[1])
            if (center.y < 400 || center.y > 900) continue
            clickNodes(center.x, center.y) {
            }
            Thread.sleep(100)
        }
    }

    private fun getScreen(): Mat {
        val screenBitmap = CaptureService.getScreenShot()

        val dir: File = getDir("screen", Context.MODE_PRIVATE)
        val file = File(dir, "lastBitmap.png")
        val fos = FileOutputStream(file)
        screenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();

        val screenBitmapHalf = cropBitmap(screenBitmap)
        val screen = Mat()
        Utils.bitmapToMat(screenBitmapHalf, screen)
        return screen
    }

    /**
     * 裁剪
     *
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
    private fun cropBitmap(bitmap: Bitmap): Bitmap? {
        // 得到图片的宽，高
        val w = bitmap.width
        val h = bitmap.height / 2
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, null, false)
    }

    private fun clickNodes(x: Double, y: Double, callback: () -> Unit) {
        if (x < 0 || y < 0) {
            Log.d("$TAG Gesture", "超出屏幕范围，取消点击")
            return
        }

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(path, 100, 50))
            .build()
        this.dispatchGesture(
            gestureDescription,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d("$TAG Gesture", "click X:${x} Y:${y}")
                    callback()
                }
            },
            null
        )
    }
}