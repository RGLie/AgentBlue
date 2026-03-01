package com.example.agentdroid.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.example.agentdroid.MainActivity
import com.example.agentdroid.R

class FloatingWindowManager(
    private val context: Context,
    private val onCommandEntered: (String) -> Unit
) {
    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val CLICK_THRESHOLD = 10
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    fun show() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_window, null)

        val layoutParams = createLayoutParams()

        floatingView?.findViewById<View>(R.id.btn_robot)
            ?.setOnTouchListener(DraggableTouchListener(layoutParams))

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "플로팅 뷰 추가 실패: ${e.message}")
        }
    }

    fun remove() {
        floatingView?.let { view ->
            windowManager?.removeView(view)
        }
        floatingView = null
        windowManager = null
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
    }

    private fun showCommandDialog() {
        val editText = EditText(context).apply {
            hint = "명령을 입력하세요"
        }

        val dialog = android.app.AlertDialog.Builder(
            context,
            android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        )
            .setTitle("AgentDroid Command")
            .setView(editText)
            .setPositiveButton("실행") { _, _ ->
                val command = editText.text.toString()
                Log.d(TAG, "명령 입력: $command")
                onCommandEntered(command)
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("설정") { _, _ ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    // This flag is required to start an activity from a service context
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private inner class DraggableTouchListener(
        private val layoutParams: WindowManager.LayoutParams
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val xDiff = (event.rawX - initialTouchX).toInt()
                    val yDiff = (event.rawY - initialTouchY).toInt()
                    if (xDiff < CLICK_THRESHOLD && yDiff < CLICK_THRESHOLD) {
                        showCommandDialog()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }
}
