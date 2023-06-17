package com.heiha.www.swippanel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * 可滑动关闭的面板, 重写onInterceptTouch, 判断是不是内容区域
 * 是内容区域：   交给嵌套滚动处理
 * 不是内容区域： 交给onInterceptTouch处理
 *
 * 如何判断是否为内容区域？
 * 内容区域由自定义View（NestedScrollBridgeView）包裹
 *
 * 1. CoordinateLayout
 *
 *
 */
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val swipePanel = SwipePanel(this)
        val contentView = LayoutInflater.from(this).inflate(R.layout.panel_content_view, null)
        swipePanel.contentView = contentView
        swipePanel.show()


//        val container = findViewById<View>(R.id.panel_container)
//        val lp = container.layoutParams as FrameLayout.LayoutParams
//        val behavior = lp.behavior as SwipeBehavior
//        behavior.draggableView = contentView

        val viewPager = contentView.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter()

    }
}

private class ViewPagerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

}

private class ViewPagerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewPagerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pager_content_view, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = 3

}
