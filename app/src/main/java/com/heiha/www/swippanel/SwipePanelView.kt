package com.heiha.www.swippanel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children

class SwipePanel(private val context: Context) {

    private val panelView: ViewGroup =
        LayoutInflater.from(context).inflate(R.layout.panel_view, null) as ViewGroup

    private val contentContainerView: ViewGroup = panelView.findViewById(R.id.panel_container)

    var contentView: View?
        set(value) {
            if (contentContainerView.childCount > 0) {
                contentContainerView.removeViewAt(0)
            }
            if (value != null) {
                contentContainerView.addView(value, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        }
        get() = contentContainerView.children.firstOrNull()

    fun show() {
        getActivity(context)?.addContentView(
            panelView, ViewGroup.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun dismiss() {
        val viewManager = panelView.parent as? ViewManager
        viewManager?.removeView(panelView)
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