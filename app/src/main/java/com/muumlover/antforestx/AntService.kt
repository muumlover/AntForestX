package com.muumlover.antforestx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

class AntService : AccessibilityService() {
    private val TAG = javaClass.name
    private var titleNow: CharSequence = ""
    private var isClicking = false
    private var clickFinish = false
    private var clickNodeList = ArrayList<AccessibilityNodeInfo>()

    companion object {
        var mService: AntService? = null
        /**
         * 辅助功能是否启动
         */
        fun isStart(): Boolean {
            return mService != null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected——无障碍服务启动成功")
        mService = this
    }

    private val nameList = listOf("蚂蚁森林", "好友排行榜")
    private val descList = listOf("地图", "成就", "通知", "背包", "任务", "攻略", "发消息", "弹幕", "浇水")

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
        if (!h5TvTitle.textOrDesc.endsWith("蚂蚁森林")) return
        Log.d(TAG, "当前页面为：<${h5TvTitle.textOrDesc}>")
        val jBarrierFree: AccessibilityNodeInfo = findNodeById(node, "J_barrier_free") ?: return
        jBarrierFree.refresh()
        Log.d(TAG, "共找到${jBarrierFree.childCount}个节点")
        for (i in 0 until jBarrierFree.childCount) {
            val child: AccessibilityNodeInfo = jBarrierFree.getChild(i) ?: continue
            val childText = child.textOrDesc
            if (childText == "" || childText in this.descList) {
                Log.d(TAG, "第 $i 个节点被忽略：<$childText>")
                continue
            }
            if ((childText.length >= 2 && childText.subSequence(0, 2) == "收集") || childText == " ") {
                Log.d(TAG, "第 $i 个节点被选中：<$childText>")
                val ball: AccessibilityNodeInfo? = jBarrierFree.getChild(i)
                if (ball != null) this.clickNodeList.add(ball)
            }
        }
        if (this.clickNodeList.count() > 0) this.clickNodes()
        else jBarrierFree.refresh()


//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
        mService = null
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "无障碍服务已关闭")
        mService = null
    }

    private fun clickNodes() {
        if (this.clickNodeList.count() == 0) {
            this.isClicking = false
            Log.d("$TAG Gesture", "结束点击元素")
            return
        }
        if (!this.isClicking) {
            this.isClicking = true
            this.clickFinish = true
            Log.d("$TAG Gesture", "开始点击元素")
        }
        val node = this.clickNodeList[0]
        val rect = Rect()
        node.getBoundsInScreen(rect)
        Log.d("$TAG Gesture", "申请点击 元素边框 $rect 元素中心 X:${rect.centerX()} Y:${rect.centerY()}")
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()
        if (x < 0 || y < 0) {
            Log.d("$TAG Gesture", "超出屏幕范围，取消点击")
            return
        }

        val path = Path()
        path.moveTo(x, y)

        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(path, 100, 50))
            .build()
        val that = this
        this.dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d("$TAG Gesture", "点击元素中心成功 X:${rect.centerX()} Y:${rect.centerY()}")
                that.clickNodeList.remove(node)
                that.clickNodes()
            }
        }, null)
    }
}