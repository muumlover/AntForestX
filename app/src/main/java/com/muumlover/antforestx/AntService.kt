package com.muumlover.antforestx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.eventTypeToString
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

fun useGestureClick(nodes: ArrayList<AccessibilityNodeInfo>, accessibilityService: AccessibilityService) {
    if (nodes.count() == 0) return
    val node = nodes[0]
    val rect = Rect()
    node.getBoundsInScreen(rect)
    Log.d("Gesture", "边框 $rect 点击中点 X:${rect.centerX()} Y:${rect.centerY()}")
    val x = rect.centerX().toFloat()
    val y = rect.centerY().toFloat()
    if (x < 0 || y < 0) {
        Log.d("Gesture", "超出屏幕范围，取消点击")
        return
    }

    val path = Path()
    path.moveTo(x, y)

    val builder = GestureDescription.Builder()
    val gestureDescription = builder
        .addStroke(GestureDescription.StrokeDescription(path, 100, 50))
        .build()
    accessibilityService.dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
            super.onCompleted(gestureDescription)
            Log.d("Gesture", "点击坐标成功 X:${rect.centerX()} Y:${rect.centerY()}")
            nodes.remove(node)
            useGestureClick(nodes, accessibilityService)
        }
    }, null)
}

class AntService : AccessibilityService() {
    private val TAG = javaClass.name

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
//        val eventType = event.eventType
//        val className = event.className
        Log.v(TAG, "${eventTypeToString(event.eventType)} :: ${event.className}")
        val node: AccessibilityNodeInfo? = event.source
        if (node != null) {
            val jBarrierFree: AccessibilityNodeInfo? = findNodeById(node, "J_barrier_free")
            if (jBarrierFree != null) {
                val ballList = ArrayList<AccessibilityNodeInfo>()
                val descList = listOf("地图", "成就", "通知", "背包", "任务", "攻略", "发消息", "弹幕", "浇水")
                for (i in 0 until jBarrierFree.childCount) {
                    val child: AccessibilityNodeInfo = jBarrierFree.getChild(i) ?: continue
                    if (child.contentDescription !in descList
                        && ((child.contentDescription.length >= 2 && child.contentDescription.subSequence(0, 2) == "收集")
                                || child.contentDescription == " ")
                    ) {
                        Log.d(TAG, "第 $i 个节点 Desc：<${child.contentDescription}>————找到发现能量球")
                        val ball: AccessibilityNodeInfo? = jBarrierFree.getChild(i)
                        if (ball != null) ballList.add(ball)
                    } else {
                        Log.d(TAG, "第 $i 个节点 Desc：<${child.contentDescription}>")
                    }
                }
                useGestureClick(ballList, this)
            } else {
                Log.d(TAG, "没有找到：J_barrier_free")
            }
        }


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
}