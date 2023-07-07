package com.heiha.www.swippanel

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.NestedScrollType
import androidx.core.view.ViewCompat.ScrollAxis
import androidx.core.view.ViewCompat.TYPE_TOUCH

class SwipableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), NestedScrollingParent3, NestedScrollingChild3 {

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val parentHelper: NestedScrollingParentHelper= NestedScrollingParentHelper(this)
    private var interceptVelocityTracker: VelocityTracker? = null
    private var interceptPointerId = -1
    private var mVelocityTracker: VelocityTracker? = null
    private var mActivePointerId = -1
    private var mLastMotionY: Int = -1
    private var mLastScrollerY = 0
    private var mNestedYOffset = 0
    private var mIsBeingDragged = false
    private val mScroller: OverScroller
    private val mTouchSlop: Int
    private val mMinimumVelocity: Int
    private val mMaximumVelocity: Int
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private val slideOutVelocity = (FLING_VELOCITY * context.resources.displayMetrics.scaledDensity).toInt()
    private var childInScrolled: Boolean? = null

    init {
        isNestedScrollingEnabled = true
        mScroller = OverScroller(context)
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setWillNotDraw(false)
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }


    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }


    override fun startNestedScroll(@ScrollAxis axes: Int): Boolean {
        return childHelper.startNestedScroll(axes)
    }


    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }


    override fun hasNestedScrollingParent(): Boolean {
        return childHelper.hasNestedScrollingParent()
    }


    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }


    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }


    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun startNestedScroll(@ScrollAxis axes: Int, @NestedScrollType type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(@NestedScrollType type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(@NestedScrollType type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?,
        @NestedScrollType type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
    }


    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?, @NestedScrollType type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, @NestedScrollType type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
    }

    private fun onNestedScrollInternal(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                       dyUnconsumed: Int, @NestedScrollType type: Int, consumed: IntArray?) {
        // 被拖动或者在拖动过程中的fling
        if (type == ViewCompat.TYPE_TOUCH) {
            val oldScrollY = scrollY
            overScrollByCompat(0,  dyUnconsumed, scrollX, scrollY, 0, getScrollRange(), 0, 0, type == TYPE_TOUCH)
            val myConsumed = scrollY - oldScrollY
            if (consumed != null) {
                consumed[1] += myConsumed
            }
            val myUnconsumed = dyUnconsumed - myConsumed
            childHelper.dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type, consumed)
        } else {
            // 子View溢出的Fling不消费
            childHelper.dispatchNestedScroll(0, 0, 0, dyUnconsumed, null, type, consumed)
        }

        childInScrolled = dyConsumed != 0
    }

    private fun onStopNestedScrollInternal(target: View, @NestedScrollType type: Int) {
        if (scrollY != 0) {
            interceptVelocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
            slide(-interceptVelocityTracker!!.getYVelocity(interceptPointerId))
        }

        childInScrolled = null
    }


    override fun onStartNestedScroll(child: View, target: View, @ScrollAxis axes: Int): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || startNestedScroll(axes)
    }


    override fun onNestedScrollAccepted(child: View, target: View, @ScrollAxis axes: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes)
    }

    override fun onStopNestedScroll(target: View) {
        parentHelper.onStopNestedScroll(target)
        stopNestedScroll()
        onStopNestedScrollInternal(target, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScrollInternal(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,  ViewCompat.TYPE_TOUCH, null)
    }


    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        dispatchNestedPreScroll(dx, dy, consumed, null)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return if (!consumed) {
            tryFling(slideOutVelocity)
            true
        } else {
            false
        } || dispatchNestedFling(0f, velocityY, true)
    }


    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    @ScrollAxis
    override fun getNestedScrollAxes(): Int {
        return parentHelper.nestedScrollAxes
    }


    override fun onStartNestedScroll(
        child: View, target: View, @ScrollAxis axes: Int,
        @NestedScrollType type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || startNestedScroll(axes, type)
    }

    override fun onNestedScrollAccepted(
        child: View, target: View, @ScrollAxis axes: Int,
        @NestedScrollType type: Int
    ) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
    }


    override fun onStopNestedScroll(target: View, @NestedScrollType type: Int) {
        parentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
        onStopNestedScrollInternal(target, type)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, @NestedScrollType type: Int
    ) {
        onNestedScrollInternal(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, null)
    }


    override fun onNestedPreScroll(
        target: View, dx: Int, dy: Int, consumed: IntArray,
        @NestedScrollType type: Int
    ) {
        if (scrollY != 0) {
            // 被拖动或者在拖动过程中的fling
                val oldScrollY = scrollY
                overScrollByCompat(0,  dy, scrollX, scrollY, 0, getScrollRange(), 0, 0, type == TYPE_TOUCH)
                val myConsumed = scrollY - oldScrollY
                if (consumed != null) {
                    consumed[1] += myConsumed
                }

        }

        dispatchNestedPreScroll(dx, dy, consumed, null, type)
    }


    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, @NestedScrollType type: Int, consumed: IntArray
    ) {
        onNestedScrollInternal(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    private fun initInterceptVelocityTrackerIfNotExists() {
        if (interceptVelocityTracker == null) {
            interceptVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleInterceptVelocityTracker() {
        if (interceptVelocityTracker != null) {
            interceptVelocityTracker!!.recycle()
            interceptVelocityTracker = null
        }
    }


    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    private fun abortAnimatedScroll() {
        mScroller.abortAnimation()
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
    }

    fun getScrollRange(): Int {
        // TODO设置可滚动的View
        var scrollRange = 0
        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = (child.layoutParams as MarginLayoutParams)
            scrollRange = height - lp.topMargin - paddingTop - paddingBottom
        }
        return scrollRange
    }


    fun tryFling(velocityY: Int) {
        if (childCount > 0 && scrollY != 0) {
            mScroller.fling(
                scrollX, scrollY,  // start
                0, velocityY,  // velocities
                0, 0, Int.MIN_VALUE, Int.MAX_VALUE,  // y
                0, 0
            ) // overscroll
            runAnimatedScroll(true)
        }
    }

    override fun computeScroll() {
        if (mScroller.isFinished) {
            return
        }
        mScroller.computeScrollOffset()
        val y = mScroller.currY
        var unconsumed = y - mLastScrollerY
        mLastScrollerY = y

        // Nested Scrolling Pre Pass
        mScrollConsumed[1] = 0
        dispatchNestedPreScroll(
            0, unconsumed, mScrollConsumed, null,
            ViewCompat.TYPE_NON_TOUCH
        )
        unconsumed -= mScrollConsumed[1]
        val range = getScrollRange()
        if (unconsumed != 0) {
            // Internal Scroll
            val oldScrollY = scrollY
            overScrollByCompat(0, unconsumed, scrollX, oldScrollY, 0, range, 0, 0, false)
            val scrolledByMe = scrollY - oldScrollY
            unconsumed -= scrolledByMe

            // Nested Scrolling Post Pass
            mScrollConsumed[1] = 0
            dispatchNestedScroll(
                0, scrolledByMe, 0, unconsumed, mScrollOffset,
                ViewCompat.TYPE_NON_TOUCH, mScrollConsumed
            )
            unconsumed -= mScrollConsumed[1]
        }
        if (unconsumed != 0) {
            abortAnimatedScroll()
        }
        if (!mScroller.isFinished) {
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
    }

    override fun scrollBy(x: Int, y: Int) {
        super.scrollBy(x, y)
    }


    private fun runAnimatedScroll(participateInNestedScrolling: Boolean) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH)
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }
        mLastScrollerY = scrollY
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun endDrag() {
        mIsBeingDragged = false
        recycleVelocityTracker()
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    fun overScrollByCompat(
        deltaX: Int, deltaY: Int,
        scrollX: Int, scrollY: Int,
        scrollRangeX: Int, scrollRangeY: Int,
        maxOverScrollX: Int, maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        var maxOverScrollX = maxOverScrollX
        var maxOverScrollY = maxOverScrollY
        var newScrollX = scrollX + deltaX
        var newScrollY = scrollY + deltaY
        var clampedY = false

        // Clamp values if at the limits and record
//        val left = -maxOverScrollX
//        val right = maxOverScrollX + scrollRangeX
        val top = 0
        val bottom = -scrollRangeY
//        var clampedX = false
//        if (newScrollX > right) {
//            newScrollX = right
//            clampedX = true
//        } else if (newScrollX < left) {
//            newScrollX = left
//            clampedX = true
//        }
//        var clampedY = false
        if (newScrollY < bottom) {
            newScrollY = bottom
            clampedY = true
        } else if (newScrollY > top) {
            newScrollY = top
            clampedY = true
        }
        scrollTo(newScrollX, newScrollY)
        return clampedY
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                recycleInterceptVelocityTracker()
                interceptPointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                interceptPointerId = ev.getPointerId(ev.actionIndex)
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_POINTER_UP -> {
                interceptPointerId = ev.getPointerId(if (ev.actionIndex == 0) 1 else 0)
            }
            MotionEvent.ACTION_CANCEL -> {

            }
        }

        initInterceptVelocityTrackerIfNotExists()
        interceptVelocityTracker?.addMovement(ev)
        return super.onInterceptTouchEvent(ev)
    }


    override fun onTouchEvent(ev: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()

        val actionMasked: Int = ev.getActionMasked()

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0
        }

        val vtev = MotionEvent.obtain(ev)
        vtev.offsetLocation(0f, mNestedYOffset.toFloat())

        run {
            when (actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (childCount == 0) {
                        return false
                    }
                    if (mIsBeingDragged) {
                        val parent = parent
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    /*
                     * If being flinged and user touches, stop the fling. isFinished
                     * will be false if being flinged.
                     */
                    if (!mScroller.isFinished()) {
                        abortAnimatedScroll()
                    }

                    // Remember where the motion event started
                    mLastMotionY = ev.getY().toInt()
                    mActivePointerId = ev.getPointerId(0)
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
                }

                MotionEvent.ACTION_MOVE -> {
                    val activePointerIndex: Int = ev.findPointerIndex(mActivePointerId)
                    if (activePointerIndex == -1) {
                        Log.e(
                            "TAG",
                            "Invalid pointerId=$mActivePointerId in onTouchEvent"
                        )
                        return@run
                    }
                    val y: Int = ev.getY(activePointerIndex).toInt()
                    var deltaY: Int = mLastMotionY - y
                    if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                        val parent = parent
                        parent?.requestDisallowInterceptTouchEvent(true)
                        mIsBeingDragged = true
                        if (deltaY > 0) {
                            deltaY -= mTouchSlop
                        } else {
                            deltaY += mTouchSlop
                        }
                    }
                    if (mIsBeingDragged) {
                        // Start with nested pre scrolling
                        if (dispatchNestedPreScroll(
                                0, deltaY, mScrollConsumed, mScrollOffset,
                                ViewCompat.TYPE_TOUCH
                            )
                        ) {
                            deltaY -= mScrollConsumed[1]
                            mNestedYOffset += mScrollOffset[1]
                        }

                        // Scroll to follow the motion event
                        mLastMotionY = y - mScrollOffset[1]
                        val oldY = scrollY
                        val range: Int = getScrollRange()
                        val overscrollMode = overScrollMode
                        val canOverscroll =
                            overscrollMode == OVER_SCROLL_ALWAYS || overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0

                        // Calling overScrollByCompat will call onOverScrolled, which
                        // calls onScrollChanged if applicable.

                        // Calling overScrollByCompat will call onOverScrolled, which
                        // calls onScrollChanged if applicable.
                        val clearVelocityTracker = overScrollByCompat(
                            0, deltaY, 0, scrollY, 0, range, 0,
                            0, true
                        ) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
                        val scrolledDeltaY = scrollY - oldY
                        val unconsumedY = deltaY - scrolledDeltaY
                        mScrollConsumed[1] = 0
                        dispatchNestedScroll(
                            0, scrolledDeltaY, 0, unconsumedY, mScrollOffset,
                            ViewCompat.TYPE_TOUCH, mScrollConsumed
                        )
                        mLastMotionY -= mScrollOffset[1]
                        mNestedYOffset += mScrollOffset[1]
                        if (clearVelocityTracker) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker!!.clear()
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val velocityTracker = mVelocityTracker!!
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                    val initialVelocity = velocityTracker.getYVelocity(mActivePointerId)
                    slide(-initialVelocity)
//                    if (Math.abs(initialVelocity) >= mMinimumVelocity) {
//                        if (!dispatchNestedPreFling(0f, -initialVelocity.toFloat())
//                        ) {
//                            dispatchNestedFling(0f, -initialVelocity.toFloat(), true)
//                            if (-initialVelocity.toFloat() < 0) {
//                                fling(slideOutVelocity)
//                            } else {
//                                fling(-slideOutVelocity)
//                            }
//
//                        }
//                    }
//                    else if (mScroller.springBack(
//                            scrollX, scrollY, 0, 0, 0,
//                            getScrollRange()
//                        )
//                    ) {
//                        ViewCompat.postInvalidateOnAnimation(this)
//                    }
                    mActivePointerId = INVALID_POINTER
                    endDrag()
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (mIsBeingDragged && childCount > 0) {
//                        if (mScroller.springBack(
//                                scrollX, scrollY, 0, 0, 0,
//                                getScrollRange()
//                            )
//                        ) {
//                            ViewCompat.postInvalidateOnAnimation(this)
//                        }
                    }
                    mActivePointerId = INVALID_POINTER
                    endDrag()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    val index: Int = ev.getActionIndex()
                    mLastMotionY = ev.getY(index).toInt()
                    mActivePointerId = ev.getPointerId(index)
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    onSecondaryPointerUp(ev)
                    mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId)).toInt()
                }
            }

        }


        if (mVelocityTracker != null) {
            mVelocityTracker!!.addMovement(vtev)
        }
        vtev.recycle()

        return true
    }

    private fun slide(velocity: Float) {
        if (-scrollY > 0.34 * getScrollRange()) {
            // 下2/3区域
            // 向上有一定的加速度，阻止滑出
            if (velocity >= mMinimumVelocity) {
                tryFling(-slideOutVelocity)
            } else {
                tryFling(slideOutVelocity)
            }
        } else {
            // 上1/3区域
            // 向下有一定的加速度，滑出
            if (-velocity >= mMinimumVelocity) {
                tryFling(slideOutVelocity)
            } else {
                tryFling(-slideOutVelocity)
            }
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionY = ev.getY(newPointerIndex).toInt()
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.clear()
            }
        }
    }


    companion object {
        private const val INVALID_POINTER = -1
        private const val FLING_VELOCITY = -3500 // dp/s
    }


}