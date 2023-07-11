# SwipePanel

**一个易用的且具有良好兼容性的可滑动面板。**

- **易用：**只对外暴露少量接口
- **良好兼容性：**对比Android官方的BottomSheet组件，BottomSheet只兼容单个嵌套滚动场景，对于PagerView嵌套NestedScrollView/RecylcerView的场景无法兼容。

## 功能演示

### show & dismiss panel

dismiss by clicking background

<video src="res/show_dismiss.mp4"></video>

### 多Pager的嵌套滚动

<video src="res/nested_scroll.mp4"></video>

## 使用

1. 定义面板内容： contentView
2. 实例化面板

```kotlin
val swipePanel = SwipePanel(context, contentView)
```

3. 展示隐藏面板

```kotlin
swipePanel.show()
swipePanle.dismiss()
```

