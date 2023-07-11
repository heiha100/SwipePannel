package com.heiha.www.swippanel

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
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
        val contentView = LayoutInflater.from(this).inflate(R.layout.panel_content_view, null)
        val swipePanel = SwipePanel(this, contentView)
        val viewPager = contentView.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter()

        findViewById<View>(R.id.poke).setOnClickListener {
            swipePanel.show()
        }
    }
}

private class ViewPagerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val pager = itemView.findViewById<View>(R.id.pager)
    private val tv = itemView.findViewById<TextView>(R.id.tv)
    private val text = "Hello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\nHello World!\n"


    fun bind(item: Int) {
        pager.setBackgroundColor(when (item) {
            0 ->  Color.parseColor("#434FA4")
            1 ->  Color.parseColor("#CA5847")
            2 -> Color.parseColor("#03DAC6")
            else -> Color.parseColor("#FFFFFF")
        })

        tv.text = when (item) {
            0 -> text.replace("Hello World!", "👋 Pager-----------1")
            1 -> text.replace("Hello World!", "😊 Pager-----------2")
            2 -> text.replace("Hello World!", "💡 Pager-----------3")
            else -> null
        }
    }
}

private class ViewPagerAdapter: RecyclerView.Adapter<ViewPagerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        return ViewPagerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pager_content_view, parent, false))
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 3

}
