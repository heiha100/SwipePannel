package com.heiha.www.swippanel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.children

class SwipePanel(private val context: Context, private val contentView: View) {

    private val swipeView: SwipeView =
        LayoutInflater.from(context).inflate(R.layout.panel_view, null) as SwipeView
    
    init {
        swipeView.swipeListener = object : SwipeView.OnSwipeListener {
            override fun onSwiping(swipeY: Int) {
                if (swipeView.isSwipeToBottom()) {
                    dismissInternal()
                }
            }
        }

        if (swipeView.childCount > 0) {
            swipeView.removeViewAt(0)
        }
        swipeView.addView(contentView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    fun show() {
        if (swipeView.parent == null) {
            getActivity(context)?.addContentView(
                swipeView, ViewGroup.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            )
        }
        swipeView.swipeToTop()
    }

    fun dismiss() {
        swipeView.swipeToBottom()
    }

    private fun dismissInternal() {
        val viewManager = swipeView.parent as? ViewManager
        viewManager?.removeView(swipeView)
    }


    private fun getActivity(context: Context?): Activity? {
        if (context == null) {
            return null
        } else if (context is ContextWrapper) {
            return if (context is Activity) {
                context
            } else {
                getActivity(context.baseContext)
            }
        }
        return null
    }
}