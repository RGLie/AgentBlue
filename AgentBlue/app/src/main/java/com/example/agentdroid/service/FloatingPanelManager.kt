package com.example.agentdroid.service

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.agentdroid.AgentStateManager
import com.example.agentdroid.R

class FloatingPanelManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingPanelManager"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var panelView: View? = null
    private var dotAnimator: ObjectAnimator? = null

    private var tvStatus: TextView? = null
    private var tvCommand: TextView? = null
    private var tvStep: TextView? = null
    private var tvReasoning: TextView? = null
    private var progressBar: ProgressBar? = null
    private var reasoningContainer: LinearLayout? = null
    private var statusDot: View? = null
    private var btnStop: TextView? = null

    private val layoutParams get() = panelView?.layoutParams as? WindowManager.LayoutParams

    fun show(command: String, maxSteps: Int) {
        handler.post { showInternal(command, maxSteps) }
    }

    fun updateStep(step: Int, maxSteps: Int, reasoning: String?) {
        handler.post { updateStepInternal(step, maxSteps, reasoning) }
    }

    fun showResult(success: Boolean, message: String) {
        handler.post { showResultInternal(success, message) }
    }

    fun dismiss() {
        handler.post { dismissInternal() }
    }

    private fun showInternal(command: String, maxSteps: Int) {
        if (panelView != null) dismissInternal()

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        panelView = LayoutInflater.from(context).inflate(R.layout.layout_floating_panel, null)

        bindViews()
        setupListeners()

        tvCommand?.text = command
        tvStatus?.text = "실행 중"
        tvStep?.text = "0/$maxSteps"
        progressBar?.max = maxSteps * 100
        progressBar?.progress = 0
        reasoningContainer?.visibility = View.GONE

        startDotAnimation()

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        try {
            windowManager?.addView(panelView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "패널 표시 실패: ${e.message}")
        }
    }

    private fun updateStepInternal(step: Int, maxSteps: Int, reasoning: String?) {
        tvStep?.text = "$step/$maxSteps"

        val targetProgress = step * 100
        val animator = ObjectAnimator.ofInt(progressBar, "progress", targetProgress)
        animator.duration = 300
        animator.start()

        if (!reasoning.isNullOrEmpty()) {
            reasoningContainer?.visibility = View.VISIBLE
            tvReasoning?.text = reasoning
        }
    }

    private fun showResultInternal(success: Boolean, message: String) {
        stopDotAnimation()

        if (success) {
            statusDot?.setBackgroundResource(R.drawable.shape_status_dot)
            tvStatus?.text = "완료"
            tvStatus?.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvStatus?.text = "실패"
            tvStatus?.setTextColor(0xFFE53935.toInt())
        }

        btnStop?.visibility = View.GONE
        reasoningContainer?.visibility = View.VISIBLE
        tvReasoning?.text = message

        handler.postDelayed({ dismissInternal() }, 5000)
    }

    private fun dismissInternal() {
        stopDotAnimation()
        panelView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) { }
        }
        panelView = null
        windowManager = null
    }

    private fun bindViews() {
        panelView?.let { view ->
            tvStatus = view.findViewById(R.id.tv_status)
            tvCommand = view.findViewById(R.id.tv_command)
            tvStep = view.findViewById(R.id.tv_step)
            tvReasoning = view.findViewById(R.id.tv_reasoning)
            progressBar = view.findViewById(R.id.progress_bar)
            reasoningContainer = view.findViewById(R.id.reasoning_container)
            statusDot = view.findViewById(R.id.status_dot)
            btnStop = view.findViewById(R.id.btn_stop)
        }
    }

    private fun setupListeners() {
        btnStop?.setOnClickListener {
            AgentStateManager.requestCancel()
            tvStatus?.text = "취소 중..."
            btnStop?.visibility = View.GONE
        }

        panelView?.findViewById<TextView>(R.id.btn_close_panel)?.setOnClickListener {
            dismissInternal()
        }

        var initialY = 0
        var initialTouchY = 0f

        panelView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = layoutParams?.y ?: 0
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.let { params ->
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(panelView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startDotAnimation() {
        statusDot?.let { dot ->
            dotAnimator = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.3f).apply {
                duration = 800
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
    }

    private fun stopDotAnimation() {
        dotAnimator?.cancel()
        dotAnimator = null
        statusDot?.alpha = 1f
    }
}
