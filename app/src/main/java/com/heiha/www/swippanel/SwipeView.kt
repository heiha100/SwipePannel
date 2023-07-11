package com.heiha.www.swippanel

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
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
import kotlin.math.abs

class SwipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), NestedScrollingParent3, NestedScrollingChild3 {

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val parentHelper: NestedScrollingParentHelper= NestedScrollingParentHelper(this)
    private var velocityTracker: VelocityTracker? = null
    private var activePointerId = -1
    private var lastMotionY: Int = -1
    private var lastScrollerY = 0

    /**
     * record offset relative to window by nested scrolling by parent view
     */
    private var nestedOffsetY = 0
    private var isBeingDraggedDirectly = false
    private val scroller: OverScroller
    private val touchSlop: Int
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private val slideOutVelocity = (FLING_VELOCITY * context.resources.displayMetrics.scaledDensity).toInt()
    private var needClearVelocityTracker = false
    private var needInvalidPointerId = false
    private val _backgroundClickListener by lazy {
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                swipeToBottom()
                return true
            }
        }
    }

    private val gestureDetector by lazy {
        GestureDetector(context, _backgroundClickListener)
    }

    /**
     * listen distance of swiping
     */
    var swipeListener: OnSwipeListener? = null

    init {
        isNestedScrollingEnabled = true
        scroller = OverScroller(context)
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setWillNotDraw(false)
        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
        minimumVelocity = configuration.scaledMinimumFlingVelocity
        maximumVelocity = configuration.scaledMaximumFlingVelocity
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

    private fun onStopNestedScrollInternal(target: View, @NestedScrollType type: Int) {
        if (scrollY != 0) {
            velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
            swipe(-velocityTracker!!.getYVelocity(activePointerId))
        }
    }


    override fun onStartNestedScroll(child: View, target: View, @ScrollAxis axes: Int): Boolean {
        return onStartNestedScrollInternal(child, target, axes, TYPE_TOUCH)
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
        onNestedPreScroll(target, dx, dy, consumed, TYPE_TOUCH)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return if (!consumed) {
            if (!scroller.isFinished) {
                abortAnimatedScroll()
            }
            tryFling(slideOutVelocity)
            true
        } else {
            false
        } || dispatchNestedFling(0f, velocityY, true)
    }


    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return if (scrollY != 0) {
            true
        } else {
            dispatchNestedPreFling(velocityX, velocityY)
        }
    }

    @ScrollAxis
    override fun getNestedScrollAxes(): Int {
        return parentHelper.nestedScrollAxes
    }


    override fun onStartNestedScroll(
        child: View, target: View, @ScrollAxis axes: Int,
        @NestedScrollType type: Int
    ): Boolean {
        return onStartNestedScrollInternal(child, target, axes, type)
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
        // intercept scroll when current view is swiping
        if (scrollY != 0) {
                val oldScrollY = scrollY
                overScrollByCompat(0,  dy, scrollX, scrollY, 0, getScrollRange(), type == TYPE_TOUCH)
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

    private fun onStartNestedScrollInternal(child: View, target: View, @ScrollAxis axes: Int,
                                                 @NestedScrollType type: Int): Boolean {
        if (!scroller.isFinished) {
            abortAnimatedScroll()
        }
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || startNestedScroll(axes, type)
    }

    private fun onNestedScrollInternal(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                       dyUnconsumed: Int, @NestedScrollType type: Int, consumed: IntArray?) {
        // don't consume fling that overflow by child view
        if (type == TYPE_TOUCH) {
            val oldScrollY = scrollY
            overScrollByCompat(0,  dyUnconsumed, scrollX, scrollY, 0, getScrollRange(), type == TYPE_TOUCH)
            val myConsumed = scrollY - oldScrollY
            if (consumed != null) {
                consumed[1] += myConsumed
            }
            val myUnconsumed = dyUnconsumed - myConsumed
            childHelper.dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type, consumed)
        } else {
            childHelper.dispatchNestedScroll(0, 0, 0, dyUnconsumed, null, type, consumed)
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    private fun abortAnimatedScroll() {
        scroller.abortAnimation()
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
        // execute fling when current view is swiping
        if (childCount > 0 && scrollY != 0) {
            scroller.fling(
                scrollX, scrollY,  // start
                0, velocityY,  // velocities
                0, 0, Int.MIN_VALUE, Int.MAX_VALUE,  // y
                0, 0
            ) // overscroll
            runAnimatedScroll(true)
        }
    }

    override fun computeScroll() {
        if (scroller.isFinished) {
            return
        }
        scroller.computeScrollOffset()
        val y = scroller.currY
        var unconsumed = y - lastScrollerY
        lastScrollerY = y

        // Nested Scrolling Pre Pass
        scrollConsumed[1] = 0
        dispatchNestedPreScroll(
            0, unconsumed, scrollConsumed, null,
            ViewCompat.TYPE_NON_TOUCH
        )
        unconsumed -= scrollConsumed[1]
        val range = getScrollRange()
        if (unconsumed != 0) {
            // Internal Scroll
            val oldScrollY = scrollY
            overScrollByCompat(0, unconsumed, scrollX, oldScrollY, 0, range, false)
            val scrolledByMe = scrollY - oldScrollY
            unconsumed -= scrolledByMe

            // Nested Scrolling Post Pass
            scrollConsumed[1] = 0
            dispatchNestedScroll(
                0, scrolledByMe, 0, unconsumed, scrollOffset,
                ViewCompat.TYPE_NON_TOUCH, scrollConsumed
            )
            unconsumed -= scrollConsumed[1]
        }
        if (unconsumed != 0) {
            abortAnimatedScroll()
        }
        if (!scroller.isFinished) {
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
        lastScrollerY = scrollY
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun endDrag() {
        isBeingDraggedDirectly = false
        recycleVelocityTracker()
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    private fun overScrollByCompat(
        deltaX: Int, deltaY: Int,
        scrollX: Int, scrollY: Int,
        scrollRangeX: Int, scrollRangeY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        var newScrollX = scrollX + deltaX
        var newScrollY = scrollY + deltaY
        var clampedY = false

        // Clamp values if at the limits and record
        val top = 0
        val bottom = -scrollRangeY
        if (newScrollY < bottom) {
            newScrollY = bottom
            clampedY = true
        } else if (newScrollY > top) {
            newScrollY = top
            clampedY = true
        }
        scrollTo(newScrollX, newScrollY)
        swipeListener?.onSwiping(- newScrollY + lastScrollerY)
        return clampedY
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()

        if (needClearVelocityTracker) {
            velocityTracker?.clear()
            needClearVelocityTracker = false
        }

        if (needInvalidPointerId) {
            activePointerId = INVALID_POINTER
            needInvalidPointerId = false
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> activePointerId = ev.getPointerId(ev.actionIndex)
            MotionEvent.ACTION_UP -> {
                needClearVelocityTracker = true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    activePointerId =
                        ev.getPointerId(if (ev.actionIndex == 0) 1 else 0)
                    needClearVelocityTracker = true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                needClearVelocityTracker = true
            }
        }

        velocityTracker?.addMovement(ev)
        return super.dispatchTouchEvent(ev)
    }


    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // detect gesture on background area
        if (ev.y >= 0 && ev.y <= height - getScrollRange()) {
            gestureDetector.onTouchEvent(ev)
        }

        initVelocityTrackerIfNotExists()

        val actionMasked: Int = ev.getActionMasked()

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0
        }

        val vtev = MotionEvent.obtain(ev)
        vtev.offsetLocation(0f, nestedOffsetY.toFloat())

        run {
            when (actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (childCount == 0) {
                        return false
                    }
                    if (isBeingDraggedDirectly) {
                        val parent = parent
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    /*
                     * If being flinged and user touches, stop the fling. isFinished
                     * will be false if being flinged.
                     */
                    if (!scroller.isFinished) {
                        abortAnimatedScroll()
                    }

                    // Remember where the motion event started
                    lastMotionY = ev.y.toInt()
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
                }

                MotionEvent.ACTION_MOVE -> {
                    val activePointerIndex: Int = ev.findPointerIndex(activePointerId)
                    if (activePointerIndex == -1) {
                        Log.e(
                            "TAG",
                            "Invalid pointerId=$activePointerId in onTouchEvent"
                        )
                        return@run
                    }
                    val y: Int = ev.getY(activePointerIndex).toInt()
                    var deltaY: Int = lastMotionY - y
                    if (!isBeingDraggedDirectly && abs(deltaY) > touchSlop) {
                        val parent = parent
                        parent?.requestDisallowInterceptTouchEvent(true)
                        isBeingDraggedDirectly = true
                        if (deltaY > 0) {
                            deltaY -= touchSlop
                        } else {
                            deltaY += touchSlop
                        }
                    }
                    if (isBeingDraggedDirectly) {
                        // Start with nested pre scrolling
                        if (dispatchNestedPreScroll(
                                0, deltaY, scrollConsumed, scrollOffset,
                                ViewCompat.TYPE_TOUCH
                            )
                        ) {
                            deltaY -= scrollConsumed[1]
                            nestedOffsetY += scrollOffset[1]
                        }

                        // Scroll to follow the motion event
                        lastMotionY = y - scrollOffset[1]
                        val oldY = scrollY
                        val range: Int = getScrollRange()

                        // Calling overScrollByCompat will call onOverScrolled, which
                        // calls onScrollChanged if applicable.
                        val clearVelocityTracker = overScrollByCompat(
                            0, deltaY, 0, scrollY, 0, range, true) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
                        val scrolledDeltaY = scrollY - oldY
                        val unconsumedY = deltaY - scrolledDeltaY
                        scrollConsumed[1] = 0
                        dispatchNestedScroll(
                            0, scrolledDeltaY, 0, unconsumedY, scrollOffset,
                            ViewCompat.TYPE_TOUCH, scrollConsumed
                        )
                        lastMotionY -= scrollOffset[1]
                        nestedOffsetY += scrollOffset[1]
                        if (clearVelocityTracker) {
                            // Break our velocity if we hit a scroll barrier.
                            velocityTracker!!.clear()
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val velocityTracker = velocityTracker!!
                    velocityTracker.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                    val initialVelocity = velocityTracker.getYVelocity(activePointerId)
                    swipe(-initialVelocity)
                    needInvalidPointerId = true
                    endDrag()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    val index: Int = ev.actionIndex
                    lastMotionY = ev.getY(index).toInt()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    lastMotionY = ev.getY(ev.findPointerIndex(activePointerId)).toInt()
                }
            }

        }


        if (velocityTracker != null) {
            velocityTracker!!.addMovement(vtev)
        }
        vtev.recycle()

        return true
    }

    private fun swipe(velocity: Float) {
        if (-scrollY > 0.34 * getScrollRange()) {
            // scroll to bottom of scrollRange * 2/3
            // if there are velocity that direct to top, then scroll back to top
            // else scroll to bottom until the edge of scrollRange
            if (velocity >= minimumVelocity) {
                tryFling(-slideOutVelocity)
            } else {
                tryFling(slideOutVelocity)
            }
        } else {
            // scroll to top of scrollRange * 1/3
            // if there are velocity that direct to bottom, then scroll to bottom
            // else scroll back to top
            if (-velocity >= minimumVelocity) {
                tryFling(slideOutVelocity)
            } else {
                tryFling(-slideOutVelocity)
            }
        }
    }


    /**
     * verify the view is swiped to top edge
     */
    fun isSwipeToTop(): Boolean = -scrollY <= 0

    fun isSwipeToBottom(): Boolean = -scrollY >= getScrollRange()

    fun swipeToTop() {
        scroller.fling(
            scrollX, scrollY,  // start
            0, 10000,  // velocities
            0, 0, Int.MIN_VALUE, Int.MAX_VALUE,  // y
            0, 0
        ) // overscroll
        runAnimatedScroll(false)
    }

    fun swipeToBottom() {
        scroller.fling(
            scrollX, scrollY,  // start
            0, -10000,  // velocities
            0, 0, Int.MIN_VALUE, Int.MAX_VALUE,  // y
            0, 0
        ) // overscroll
        runAnimatedScroll(false)
    }

    companion object {
        private const val INVALID_POINTER = -1
        private const val FLING_VELOCITY = -3500 // dp/s
    }

    interface OnSwipeListener {
        fun onSwiping(swipeY: Int)
    }


}