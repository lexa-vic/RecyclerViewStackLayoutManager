package com.kostikov.recyclerviewexamples

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View


/**
 * @author Kostikov Aleksey.
 */
class ZoomListLayoutManager: RecyclerView.LayoutManager() {

    private val VIEW_HEIGHT_PERCENT = 0.75f
    private val SCALE_THRESHOLD_PERCENT = 0.66f

    private val viewCache = SparseArray<View>()


    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)
        fill(recycler)
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val delta = scrollVerticallyInternal(dy)

        offsetChildrenVertical(-delta)
        fill(recycler)
        return delta
    }

    private fun fill(recycler: RecyclerView.Recycler) {

        val anchorView = getAnchorView()
        viewCache.clear()

        //Помещаем вьюшки в кэш и...
        for (i in 0 until childCount){
            val view = getChildAt(i)
            val pos = getPosition(view)
            viewCache.put(pos, view)
        }

        //... и удалям из лэйаута
        for (i in 0 until viewCache.size()) {
            detachView(viewCache.valueAt(i))
        }

        fillUp(anchorView, recycler)
        fillDown(anchorView, recycler)

        //отправляем в корзину всё, что не потребовалось в этом цикле лэйаута
        //эти вьюшки или ушли за экран или не понадобились, потому что соответствующие элементы
        //удалились из адаптера
        for (i in 0 until viewCache.size()) {
            recycler.recycleView(viewCache.valueAt(i))
        }

        updateViewScale()

    }

    private fun fillUp(anchorView: View?, recycler: RecyclerView.Recycler) {
        var anchorPos = 0
        var anchorTop = 0
        if (anchorView != null) {
            anchorPos = getPosition(anchorView)
            anchorTop = getDecoratedTop(anchorView)
        }

        var fillUp = true
        var pos = anchorPos - 1
        var viewBottom = anchorTop //нижняя граница следующей вьюшки будет начитаться от верхней границы предыдущей
        val viewHeight = (height * VIEW_HEIGHT_PERCENT).toInt()
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY)
        while (fillUp && pos >= 0) {
            var view = viewCache.get(pos) //проверяем кэш
            if (view == null) {
                //если вьюшки нет в кэше - просим у recycler новую, измеряем и лэйаутим её
                view = recycler.getViewForPosition(pos)
                addView(view!!, 0)
                measureChildWithMarginsAndDecorators(view, widthSpec, heightSpec)

                val decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, 0, viewBottom - viewHeight, decoratedMeasuredWidth, viewBottom)


            } else {
                //если вьюшка есть в кэше - просто аттачим её обратно
                //нет необходимости проводить measure/layout цикл.
                attachView(view)
                viewCache.remove(pos)
            }
            viewBottom = getDecoratedTop(view)
            fillUp = viewBottom > 0
            pos--
        }
    }

    private fun fillDown(anchorView: View?, recycler: RecyclerView.Recycler) {
        var anchorPos = 0
        var anchorTop = 0
        if (anchorView != null) {
            anchorPos = getPosition(anchorView)
            anchorTop = getDecoratedTop(anchorView)
        }

        var pos = anchorPos
        var fillDown = true
        val height = height
        var viewTop = anchorTop
        val itemCount = itemCount
        val viewHeight = (getHeight() * VIEW_HEIGHT_PERCENT).toInt()

        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY)

        while (fillDown && pos < itemCount) {
            var view = viewCache.get(pos)
            if (view == null) {
                view = recycler.getViewForPosition(pos)
                addView(view)
                measureChildWithMarginsAndDecorators(view, widthSpec, heightSpec)
                /*val bounds = Rect()
                getDecoratedBoundsWithMargins(view, bounds)
                layoutDecorated(view, -bounds.left, viewTop - bounds.top, width-bounds.right, viewTop + viewHeight - bounds.bottom)*/

                val decoratedMeasuredWidth = getDecoratedMeasuredWidth(view)
                layoutDecorated(view, 0, viewTop, decoratedMeasuredWidth, viewTop + viewHeight)
            } else {
                attachView(view)
                viewCache.remove(pos)
            }
            viewTop = getDecoratedBottom(view)
            fillDown = viewTop <= height
            pos++
        }
    }

    //метод вернет вьюшку с максимальной видимой площадью
    private fun getAnchorView(): View? {
        val childCount = childCount
        val viewsOnScreen = mutableMapOf<Int, View>()

        val mainRect = Rect(0, 0, width, height)
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val top = getDecoratedTop(view)
            val bottom = getDecoratedBottom(view)
            val left = getDecoratedLeft(view)
            val right = getDecoratedRight(view)
            val viewRect = Rect(left, top, right, bottom)

            val intersect = viewRect.intersect(mainRect)
            if (intersect) {
                val square = viewRect.width() * viewRect.height()
                viewsOnScreen[square] = view
            }

        }
        if (viewsOnScreen.isEmpty()) {
            return null
        }
        var maxSquare: Int? = null

        for (square in viewsOnScreen.keys) {
            if (maxSquare == null) {
                maxSquare = square
            } else {
                maxSquare = Math.max(maxSquare, square!!)
            }
        }
        return viewsOnScreen.get(maxSquare)
    }

    private fun scrollVerticallyInternal(dy: Int): Int {
        val childCount = childCount
        val itemCount = itemCount
        if (childCount == 0) {
            return 0
        }

        val topView = getChildAt(0)
        val bottomView = getChildAt(childCount - 1)

        //Случай, когда все вьюшки поместились на экране
        val viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView)
        if (viewSpan <= height) {
            return 0
        }

        var delta = 0
        //если контент уезжает вниз
        if (dy < 0) {
            val firstView = getChildAt(0)
            val firstViewAdapterPos = getPosition(firstView)
            if (firstViewAdapterPos > 0) { //если верхняя вюшка не самая первая в адаптере
                delta = dy
            } else { //если верхняя вьюшка самая первая в адаптере и выше вьюшек больше быть не может
                val viewTop = getDecoratedTop(firstView)
                delta = Math.max(viewTop, dy)
            }
        } else if (dy > 0) { //если контент уезжает вверх
            val lastView = getChildAt(childCount - 1)
            val lastViewAdapterPos = getPosition(lastView)
            if (lastViewAdapterPos < itemCount - 1) { //если нижняя вюшка не самая последняя в адаптере
                delta = dy
            } else { //если нижняя вьюшка самая последняя в адаптере и ниже вьюшек больше быть не может
                val viewBottom = getDecoratedBottom(lastView)
                val parentBottom = height
                delta = Math.min(viewBottom - parentBottom, dy)
            }
        }
        return delta
    }

    private fun measureChildWithMarginsAndDecorators(view: View, widthSpec: Int, heightSpec: Int){
        val rect = Rect()
        calculateItemDecorationsForChild(view, rect)

        val lp = view.layoutParams as RecyclerView.LayoutParams

        val updWidthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + rect.left,
                lp.rightMargin + rect.right)
        val updHeightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + rect.top,
                lp.bottomMargin + rect.bottom)

        view.measure(updWidthSpec, updHeightSpec)
    }

    private fun updateSpecWithExtra(spec: Int, startInset: Int, endInset: Int): Int {
        if (startInset == 0 && endInset == 0) {
            return spec
        }
        val mode = View.MeasureSpec.getMode(spec)

        return if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode)
        } else spec
    }

    private fun updateViewScale() {
        val childCount = childCount
        val height = height
        val thresholdPx = (height * SCALE_THRESHOLD_PERCENT).toInt()
        for (i in 0 until childCount){
            var scale = 1f
            val view = getChildAt(i)
            val viewTop = getDecoratedTop(view)
            if (viewTop >= thresholdPx) {
                val delta = viewTop - thresholdPx
                scale = (height - delta) / height.toFloat()
                scale = Math.max(scale, 0f)
            }
            view.pivotX = (view.height / 2).toFloat()
            view.pivotY = (view.height / -2).toFloat()
            view.scaleX = scale
            view.scaleY = scale
        }

    }
}
