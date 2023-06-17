package com.heiha.www.swippanel

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import kotlin.math.max

class SwipeBehavior(context: Context, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<NestedScrollBridgeView>(context, attrs) {
    private var isDragging = false
    private var isSettling = false
    var draggableView: View? = null
    private var dismissPosition: Float = 0.3F
    private var dismissVelocity: Float = 100F
    private var settlingAnimator: ViewPropertyAnimator? = null
    private lateinit var velocityTracker: VelocityTracker


    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: NestedScrollBridgeView,
        ev: MotionEvent
    ): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            velocityTracker.clear()
        }
        velocityTracker.addMovement(ev)

        if ((ev.action == MotionEvent.ACTION_CANCEL || ev.action == MotionEvent.ACTION_DOWN) && isDragging) {

        }
        return super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollBridgeView,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollBridgeView,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (isDragging) {
            onDrag(dy, consumed)
        } else {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        }
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollBridgeView,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyConsumed == 0 && dyUnconsumed < 0 && !isDragging) {
            isDragging = true
        }

        if (isDragging) {
            onDrag(dyUnconsumed, consumed)
        }
    }

    private fun onDrag(
        dyUnconsumed: Int, consumed: IntArray
    ) {
        if (isSettling) {
            isSettling = false
            settlingAnimator?.cancel()
        }

        draggableView?.apply {
            val preTY = translationY
            val targetY = max(0F, translationY - dyUnconsumed)
            translationY = targetY
            consumed[1] += (preTY - targetY).toInt()
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollBridgeView,
        target: View,
        type: Int
    ) {
        if (isDragging) {
            draggableView?.let { draggableView ->
                isSettling = true
                velocityTracker.computeCurrentVelocity(1000)
                val vY = velocityTracker.yVelocity / 100
                if (vY > dismissVelocity) {
                    dismiss()
                } else if (-vY < ViewConfiguration.getMinimumFlingVelocity() && draggableView.translationY >= draggableView.measuredHeight * dismissPosition) {
                    dismiss()
                } else {
                    resume()
                }

            }

        }

        isDragging = false

    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollBridgeView,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        if (!isSettling && !consumed && velocityY > dismissVelocity) {
            isSettling = true
            dismiss()
        }
        return true
    }

    private fun dismiss() {
        draggableView?.apply {
            settlingAnimator = animate()
            settlingAnimator!!.translationY(measuredHeight.toFloat())
                .setDuration(((measuredHeight - translationY) / dismissVelocity * 10).toLong())
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        isSettling = false
                        // TODO 回调
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        isSettling = false
                    }

                }).start()
        }
    }

    private fun resume() {
        draggableView?.apply {
            settlingAnimator = animate()
            settlingAnimator!!.translationY(0F)
                .setDuration((translationY / dismissVelocity * 10).toLong())
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        isSettling = false
                        // TODO 回调
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        isSettling = false
                    }

                }).start()
        }
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        velocityTracker = VelocityTracker.obtain()
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        velocityTracker.recycle()
    }

}

private open class SimpleAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator?) {

    }

    override fun onAnimationEnd(animation: Animator?) {

    }

    override fun onAnimationCancel(animation: Animator?) {

    }

    override fun onAnimationRepeat(animation: Animator?) {

    }

}
