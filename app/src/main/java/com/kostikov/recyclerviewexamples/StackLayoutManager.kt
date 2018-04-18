package com.kostikov.recyclerviewexamples


import android.content.res.Resources
import android.support.animation.DynamicAnimation
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.kostikov.recyclerviewexamples.StackLayoutManager
import java.util.*

/**
 * LayoutManager реализующий складывание элементов в стек сверху и снизу экрана, а так же анимацию при окончании прокрутки.
 *
 * @author Kostikov Aleksey.
 */
class StackLayoutManager : RecyclerView.LayoutManager() {

    /**
     * Высота и ширина элементов
     */
    private var mDecoratedChildWidth: Int = 0
    private var mDecoratedChildHeight: Int = 0

    /**
     * Margins элементов
     */
    private var mLeftMargin: Int = 0
    private var mTopMargin: Int = 0
    private var mBottomMargin: Int = 0

    /**
     * Максимальное кол-во вкладок(элементов) в стеках
     */
    private var mMaxElementsInStack = 0

    /**
     * Высота вкладки элементв в стеке в пикселях
     */
    private var mItemHeightInStackInPx: Int = 0

    /**
     * Кэш вьюх видимых в текущий момент на экране
     */
    private val mViewCache = SparseArray<View>()

    /**
     * Статус скрола
     */
    private var mScrollState = ScrollState.SCROLL_NA

    private val mSpringForce = SpringForce()

    private val mAnimations = ArrayList<SpringAnimation>()

    /**
     * Возвращает якорную вьюху относительно которой раскрываются стеки.
     * Первая вьюха чья верхняя граница больше границы верхнего стека
     *
     * @return View
     */
    private val anchorView: View?
        get() {
            var view: View? = null

            for (i in 0 until mViewCache.size()) {
                if (getDecoratedTop(mViewCache.valueAt(i)) >= bottomEdgeOfTopStack) {
                    view = mViewCache.valueAt(i)
                    break
                }
            }
            return view
        }

    /**
     * Возвращает первуя вьюху до нижнего стека
     *
     * @return View
     */
    private val firstViewBeforeBottomStack: View?
        get() {
            var view: View? = null

            for (i in mViewCache.size() - ONE_ELEMENT_OFFSET downTo 0) {
                if (getDecoratedTop(mViewCache.valueAt(i)) <= topEdgeOfBottomStack) {
                    view = mViewCache.valueAt(i)
                    break
                }
            }
            return view
        }

    /**
     * Возвращает нижнюю гранизу нижнего стека
     * Расчитывается относительно высоты родителя
     *
     * @return int граница стека.
     */
    private val bottomEdgeOfTopStack: Int
        get() = height / RELATIVE_SCREEN_PART_TO_STACK

    /**
     * Возвращает текущее количество элементов в верхнем стеке
     *
     * @return int кол-во элементов в стеке
     */
    private val topStackSize: Int
        get() {
            var cnt = 0

            for (i in 0 until mViewCache.size()) {
                val view = mViewCache.valueAt(i)

                if (view.top < bottomEdgeOfTopStack) {
                    cnt++
                }
            }

            return cnt
        }

    /**
     * Возвращает текущее количество элементов в верхнем стеке
     *
     * @return int кол-во элементов в стеке
     */
    private val bottomStackSize: Int
        get() {
            var cnt = 0

            for (i in 0 until mViewCache.size()) {
                val view = mViewCache.valueAt(i)

                if (view.top >= topEdgeOfBottomStack) {
                    cnt++
                }
            }

            return cnt
        }

    /**
     * Возвращает верхнуюю границу нижнего стека
     * Расчитывается относительно высоты родителя
     *
     * @return int граница стека.
     */
    private val topEdgeOfBottomStack: Int
        get() = height - height / RELATIVE_SCREEN_PART_TO_STACK

    init {
        mSpringForce.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY).stiffness = SpringForce.STIFFNESS_LOW
    }

    /**
     * {@inheritDoc}
     */
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /**
     * {@inheritDoc}
     */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {

        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (childCount == 0 && state!!.isPreLayout) {
            return
        }

        if (childCount == 0) {
            val scrap = recycler!!.getViewForPosition(0)
            addView(scrap)
            measureChildWithMargins(scrap, 0, 0)

            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap)

            mItemHeightInStackInPx = convertDpToPx(scrap.resources, ITEM_HEIGHT_IN_STACK_IN_DP)
            mMaxElementsInStack = bottomEdgeOfTopStack / mItemHeightInStackInPx


            val layoutParams = scrap.layoutParams as RecyclerView.LayoutParams
            mLeftMargin = layoutParams.leftMargin
            mTopMargin = layoutParams.topMargin
            mBottomMargin = layoutParams.bottomMargin

            detachAndScrapView(scrap, recycler)
            recycler.recycleView(scrap)
        }


        fillViewCache()
        var startViewPosition = paddingTop
        var startAdapterPosition = -ONE_ELEMENT_OFFSET

        // Делаем detach всех view на экране, помещаем в Scrap
        detachAndScrapAttachedViews(recycler)

        // При сворачивании/разворачивании, переходе с другого экрана сохраняем элементы на своих местах
        if (mViewCache.size() != 0) {
            var view = anchorView
            if (view != null) {
                expandStack(view)

                view = mViewCache.valueAt(0)
                startViewPosition = getDecoratedTop(view!!) - mTopMargin
                startAdapterPosition = mViewCache.keyAt(0) - ONE_ELEMENT_OFFSET
            }
            mViewCache.clear()
        }

        addItemsUpperAdapterPos(recycler!!, startAdapterPosition, startViewPosition)
        createTopStackScrollDown(recycler)
        createBottomStackScrollDown()

        attachViewCache()

        mScrollState = ScrollState.SCROLL_NA
    }

    /**
     * {@inheritDoc}
     */
    override fun canScrollVertically(): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val delta: Int

        if (childCount == 0 && dy == 0) {
            return 0
        }

        if (!mAnimations.isEmpty()) {
            return dy
        }

        // Двигаем элементы если скролл закончился
        if (moveViewWhenScrollEnd(dy)) {
            return 0
        }

        fillViewCache()

        if (dy > 0) {
            delta = scrollDown(dy, recycler!!)
            mScrollState = if (delta == 0) ScrollState.SCROLL_DOWN_END else ScrollState.SCROLL_NA
        } else {
            delta = scrollUp(dy, recycler!!)
            mScrollState = if (delta == 0) ScrollState.SCROLL_UP_END else ScrollState.SCROLL_NA
        }

        attachViewCache()

        return dy
    }

    /**
     * Тут смотрим нужно ли запустить анимацию возврата на исходные позиции элементов при отпускании скролла
     * когда все элементы видны и мы тянем сверх нормы.
     */
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        // При отпускании запускаем анимацию для всех нужных вьюх
        if (state == SCROLL_STATE_IDLE) {
            animateAllMovedView()
        }
    }

    /**
     * Переместить элементы за скролом, когда скрол закончился и все элементы показаны.
     * Элементы тянуться за скролом, а после отпускания анимированного возвращаются обратно.
     *
     * @param dy пермещение по вертикали
     * @return true - элементы передвинуты, больше ничего вызывать не надо. Из метода scrollVerticallyBy можно выходить
     */
    private fun moveViewWhenScrollEnd(dy: Int): Boolean {
        var result = false

        if (mScrollState == ScrollState.SCROLL_UP_END && dy < 0) {
            moveWhenScrollUpEnd(dy)
            result = true
        } else if (mScrollState == ScrollState.SCROLL_DOWN_END && dy > 0) {
            moveWhenScrollDownEnd(dy)
            result = true
        } else {
            animateAllMovedView()
        }

        return result
    }

    /**
     * Передвижение элементов, когда скролл вниз закончен
     *
     * @param dy смещение
     */
    private fun moveWhenScrollDownEnd(dy: Int) {
        var topLimit = (paddingTop
                + mTopMargin + mItemHeightInStackInPx + mItemHeightInStackInPx / 2 * (childCount - 2))

        for (i in childCount - ONE_ELEMENT_OFFSET downTo 1) {
            val view = getChildAt(i)

            if (isViewNeedToMove(view, i)) {
                calcAndSetTranslation(dy, topLimit, view)
            }

            if (i == childCount - ONE_ELEMENT_OFFSET) {
                topLimit -= mItemHeightInStackInPx
            } else {
                topLimit -= mItemHeightInStackInPx / 2
            }
        }
    }

    /**
     * Передвижение элементов, когда скролл вверх закончен
     *
     * @param dy смещение
     */
    private fun moveWhenScrollUpEnd(dy: Int) {
        var topLimit = height - mItemHeightInStackInPx - mItemHeightInStackInPx / 2 * (childCount - ONE_ELEMENT_OFFSET)

        for (i in 0 until childCount) {
            val view = getChildAt(i)

            if (isViewNeedToMove(view, i)) {

                calcAndSetTranslation(dy, topLimit, view)
            }
            if (i == 0) {
                topLimit += mItemHeightInStackInPx
            } else {
                topLimit += mItemHeightInStackInPx / 2
            }
        }
    }

    /**
     * Расчитать и применить доспутимое смещение для анимации конца скрола
     *
     * @param dy смещение
     * @param topLimit верхняя допустимая граница
     * @param view View которую двигаем
     */
    private fun calcAndSetTranslation(dy: Int, topLimit: Int, view: View) {
        val currentTop = getDecoratedTop(view)

        val currentTranslation = Math.round(view.translationY)
        val futureTranslation = currentTranslation - dy
        val translation: Int
        // При скроле вверх
        if (dy < 0) {
            translation = if (currentTop + futureTranslation >= topLimit)
                topLimit - currentTop
            else
                futureTranslation
        } else {
            // При скроле вниз
            translation = if (currentTop + futureTranslation <= topLimit)
                topLimit - currentTop
            else
                futureTranslation
        }

        view.translationY = translation.toFloat()
    }

    /**
     * Задать анимацию возврата для всех требуемых элементов в зависимости от типа (статуса) скрола
     */
    private fun animateAllMovedView() {
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view.translationY != 0f) {
                startReturnAnimation(view)
            }
        }
    }

    /**
     * Возвращает булевое значение, говорящее нужно ли двигаеть view при данном статусе скрола
     *
     * @param view View
     * @return boolean true - вью надо двигать (движение сверх ограничения, когда все элементы видны)
     */
    private fun isViewNeedToMove(view: View, childPosition: Int): Boolean {
        var result = false

        when (mScrollState) {
            StackLayoutManager.ScrollState.SCROLL_DOWN_END -> {
                result = true

                if (childPosition < childCount - ONE_ELEMENT_OFFSET) {
                    result = getDecoratedTop(getChildAt(childPosition + ONE_ELEMENT_OFFSET)) - getDecoratedTop(view) >= mItemHeightInStackInPx
                }
            }
            StackLayoutManager.ScrollState.SCROLL_UP_END -> {
                result = true

                if (childPosition > 0) {
                    result = getDecoratedTop(view) - getDecoratedTop(getChildAt(childPosition - ONE_ELEMENT_OFFSET)) >= mItemHeightInStackInPx
                }
            }
            else -> {
            }
        }

        return result
    }

    /**
     * Запустить анимацию возврата на исходное место
     *
     * @param view View которое будет анимированно
     */
    private fun startReturnAnimation(view: View) {
        val springAnimation = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f)
        mSpringForce.finalPosition = 0f
        springAnimation.spring = mSpringForce
        mAnimations.add(springAnimation)

        springAnimation.addEndListener { animation, canceled, value, velocity -> mAnimations.remove(animation) }

        springAnimation.start()
    }

    /**
     * Заполнение mViewCache: добавляет видимые вьюхи и делает detach
     */
    private fun fillViewCache() {
        mViewCache.clear()

        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val position = getPosition(view)
            mViewCache.put(position, view)
        }

        for (i in 0 until mViewCache.size()) {
            detachView(mViewCache.valueAt(i))
        }
    }

    /**
     * Attach всех вьюх в mViewCache
     */
    private fun attachViewCache() {
        for (i in 0 until mViewCache.size()) {
            attachView(mViewCache.valueAt(i))
        }
    }

    /**
     * Скрол для просмотра нижних элементов - нижние эелементы смещаются вверх
     *
     * @param dy       - расстояние на которое пользователь хочет переместиться
     * @param recycler RecyclerView.Recycler для удаления ненужных элементов
     * @return int реально пройденное расстояние
     */
    private fun scrollDown(dy: Int, recycler: RecyclerView.Recycler): Int {
        val currentPosition: Int
        var delta = -dy

        val anchorView = anchorView ?: return 0

// Раскрываем стеки превращаем в обычный список
        expandStack(anchorView)

        currentPosition = mViewCache.keyAt(mViewCache.size() - ONE_ELEMENT_OFFSET)

        // Появился последний элемент, урезаем delta до момента когда полностью виден последний элемент
        if (mViewCache.keyAt(mViewCache.size() - ONE_ELEMENT_OFFSET) == itemCount - ONE_ELEMENT_OFFSET) {
            val lastItemBottomEdge = getDecoratedBottom(mViewCache.get(itemCount - ONE_ELEMENT_OFFSET)) + mBottomMargin
            val futureEdge = lastItemBottomEdge + delta
            delta = if (futureEdge - height >= 0) delta else height - lastItemBottomEdge
        }

        // Скролим
        for (i in 0 until mViewCache.size()) {
            mViewCache.valueAt(i).offsetTopAndBottom(delta)
        }

        val view = mViewCache.get(currentPosition)
        val baseItemDecoratedBottom = getDecoratedBottom(view)

        // Добавляем необходимые элементы после скрола
        addItemsUpperAdapterPos(recycler, currentPosition, baseItemDecoratedBottom)
        // Сворачиваем в верхний стек
        createTopStackScrollDown(recycler)
        // Сворачиваем в нижний стек
        createBottomStackScrollDown()

        return delta
    }

    /**
     * Скрол для просмотра верхних элементов - верхние эелементы смещаются вниз
     *
     * @param dy       - расстояние на которое пользователь хочет переместиться
     * @param recycler RecyclerView.Recycler для удаления ненужных элементов
     * @return int реально пройденное расстояние
     */
    private fun scrollUp(dy: Int, recycler: RecyclerView.Recycler): Int {
        val currentPosition: Int
        var delta = -dy

        val anchorView = anchorView ?: return 0

// Раскрываем стеки превращаем в обычный список
        expandStack(anchorView)

        currentPosition = mViewCache.keyAt(0)

        // Появился первый элемент, урезаем delta до момента когда полностью виден последний элемент
        if (mViewCache.get(0) != null) {
            val firstItemTopEdge = getDecoratedTop(mViewCache.get(0))
            val futureEdge = firstItemTopEdge + delta
            delta = if (mTopMargin + paddingTop - futureEdge >= 0) delta else mTopMargin + paddingTop - firstItemTopEdge
        }

        // Скролим
        for (i in 0 until mViewCache.size()) {
            mViewCache.valueAt(i).offsetTopAndBottom(delta)
        }

        val view = mViewCache.get(currentPosition)
        val baseItemDecoratedTop = getDecoratedTop(view)

        // Добавляем необходимые элементы после скрола
        addItemsLowerAdapterPos(recycler, currentPosition, baseItemDecoratedTop)

        createTopStackScrollUp()
        createBottomStackScrollUp(recycler)

        return delta
    }

    /**
     * Свернуть элементы в верхний стек при прокрутке вверх
     */
    private fun createTopStackScrollUp() {
        var edgeLimit = mTopMargin + paddingTop
        var startPosIdx = 0
        val topStackSize = topStackSize

        if (topStackSize > mMaxElementsInStack) {
            startPosIdx = topStackSize - mMaxElementsInStack
        }

        for (i in startPosIdx downTo 0) {
            val offset = if (edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0) edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) else 0
            mViewCache.valueAt(i).offsetTopAndBottom(offset)
        }

        val anchorView = mViewCache.valueAt(topStackSize)
        val topEdge = getDecoratedTop(anchorView)

        edgeLimit = mTopMargin + paddingTop + (Math.min(topStackSize, mMaxElementsInStack) - ONE_ELEMENT_OFFSET) * mItemHeightInStackInPx
        edgeLimit = if (topEdge - edgeLimit > mItemHeightInStackInPx) edgeLimit else topEdge - mItemHeightInStackInPx

        for (i in topStackSize - ONE_ELEMENT_OFFSET downTo startPosIdx) {
            val offset = if (edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0) edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) else 0
            mViewCache.valueAt(i).offsetTopAndBottom(offset)

            if (topStackSize <= mMaxElementsInStack) {
                edgeLimit = mTopMargin + paddingTop + (i - ONE_ELEMENT_OFFSET) * mItemHeightInStackInPx
            } else {
                edgeLimit = getDecoratedTop(mViewCache.valueAt(i)) - mItemHeightInStackInPx
            }

        }
    }

    /**
     * Свернуть элементы в нижний стек при прокрутке вверх
     *
     * @param recycler RecyclerView.Recycler для удаления ненужных элементов
     */
    private fun createBottomStackScrollUp(recycler: RecyclerView.Recycler) {
        val bottomStackSize = bottomStackSize
        var edgeLimit: Int
        val startPosIdx: Int

        val anchorView = firstViewBeforeBottomStack ?: return

        val topEge = getDecoratedTop(anchorView)
        startPosIdx = mViewCache.indexOfValue(anchorView) + ONE_ELEMENT_OFFSET

        val maxVisibleStackIdx = Math.min(startPosIdx + mMaxElementsInStack, mViewCache.size())

        edgeLimit = height - mItemHeightInStackInPx * (maxVisibleStackIdx - startPosIdx)
        edgeLimit = if (edgeLimit - topEge > mItemHeightInStackInPx) edgeLimit else topEge + mItemHeightInStackInPx

        for (i in startPosIdx until maxVisibleStackIdx) {
            val currentTop = getDecoratedTop(mViewCache.valueAt(i))
            val offset = if (edgeLimit - currentTop >= 0) 0 else edgeLimit - currentTop

            mViewCache.valueAt(i).offsetTopAndBottom(offset)

            if (bottomStackSize >= mMaxElementsInStack) {
                edgeLimit = getDecoratedTop(mViewCache.valueAt(i)) + mItemHeightInStackInPx
            } else {
                edgeLimit = height - mItemHeightInStackInPx * (mViewCache.size() - i - ONE_ELEMENT_OFFSET)
            }
        }

        for (i in startPosIdx + mMaxElementsInStack until mViewCache.size()) {
            recycler.recycleView(mViewCache.valueAt(i))

            mViewCache.remove(mViewCache.keyAt(i))
        }
    }

    /**
     * Свернуть элементы в нижний стек при прокрутке вниз
     */
    private fun createBottomStackScrollDown() {
        val anchorView: View?
        var edgeLimit: Int
        val anchorPos: Int

        // Первый элемент чей верхний край выходит за нижний стек, он вытягивается и как только вытянулся начинают ехать другие элементы за ним
        anchorView = firstViewBeforeBottomStack

        if (anchorView != null) {

            anchorPos = getPosition(anchorView)

            // Элементы лежащие в видимом стеке
            val stack = SparseArray<View>()
            var prevItemBottom = getDecoratedBottom(anchorView) + mBottomMargin + mTopMargin

            run {
                var i = anchorPos + ONE_ELEMENT_OFFSET
                while (i <= anchorPos + mMaxElementsInStack && i < itemCount) {
                    stack.put(i, mViewCache.get(i))
                    i++
                }
            }

            edgeLimit = height - mItemHeightInStackInPx * stack.size()
            // Верхний элемент в стеке не может быть ближе к anchorView чем mItemHeightInStackInPx
            edgeLimit = if (edgeLimit - getDecoratedTop(anchorView) > mItemHeightInStackInPx)
                edgeLimit
            else
                getDecoratedTop(anchorView) + mItemHeightInStackInPx
            val bottomStackSize = bottomStackSize

            for (i in 0 until stack.size()) {

                val currentTop = getDecoratedTop(stack.valueAt(i))

                val offset = if (edgeLimit - prevItemBottom >= 0) 0 else edgeLimit - currentTop
                stack.valueAt(i).offsetTopAndBottom(offset)

                if (bottomStackSize < mMaxElementsInStack) {
                    // Если стек заканчивается, то элементы не тянем на верх
                    edgeLimit = height - mItemHeightInStackInPx * (stack.size() - i - ONE_ELEMENT_OFFSET)
                } else {
                    edgeLimit = getDecoratedTop(stack.valueAt(i)) + mItemHeightInStackInPx
                }
                prevItemBottom = getDecoratedBottom(stack.valueAt(i))
            }
        }
    }

    /**
     * Свернуть элементы в верхний стек при прокрутке вниз
     *
     * @param recycler RecyclerView.Recycler для удаления ненужных элементов
     */
    private fun createTopStackScrollDown(recycler: RecyclerView.Recycler) {
        val anchorView: View?
        var edgeLimit: Int
        anchorView = this.anchorView

        if (anchorView == null) {
            return
        }

        val anchorPos = getPosition(anchorView)

        if (anchorPos > mMaxElementsInStack) {

            edgeLimit = mTopMargin + paddingTop

            val firstPosInStack = anchorPos - mMaxElementsInStack - ONE_ELEMENT_OFFSET

            var offset = edgeLimit - getDecoratedTop(mViewCache.get(firstPosInStack))
            // Первый элемент оставляем на своем месте на него будут заезжать другие карты
            mViewCache.get(firstPosInStack).offsetTopAndBottom(offset)

            // Последний элемент стека оставляем на месте или двигаем если сместился выше других вкладок в стеке
            edgeLimit = paddingTop + mTopMargin + mItemHeightInStackInPx * (mMaxElementsInStack - ONE_ELEMENT_OFFSET)
            offset = if (edgeLimit - getDecoratedTop(mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET)) >= 0)
                edgeLimit - getDecoratedTop(mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET))
            else
                0
            mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET).offsetTopAndBottom(offset)

            // Смещаем другие карты на первую относительно последней в стеке
            for (i in anchorPos - 2 downTo firstPosInStack + 1) {
                edgeLimit = getDecoratedTop(mViewCache.get(i + ONE_ELEMENT_OFFSET)) - mItemHeightInStackInPx
                offset = if (edgeLimit - getDecoratedTop(mViewCache.get(i)) >= 0) edgeLimit - getDecoratedTop(mViewCache.get(i)) else 0
                mViewCache.get(i).offsetTopAndBottom(offset)
            }

            for (i in 0 until firstPosInStack) {

                if (mViewCache.get(i) != null) {
                    recycler.recycleView(mViewCache.get(i))
                    mViewCache.remove(i)
                }
            }
        } else {
            edgeLimit = mTopMargin + paddingTop
            // Складываем в стек
            for (i in 0 until topStackSize) {

                val offset = if (edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0) edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) else 0
                mViewCache.valueAt(i).offsetTopAndBottom(offset)
                edgeLimit = getDecoratedTop(mViewCache.valueAt(i)) + mItemHeightInStackInPx
            }
        }
    }

    /**
     * Добавить на экран из адаптера элементы выше(меньше индекс в адаптере) базового элемента
     *
     * @param recycler               RecyclerView.Recycler для доступа к адаптеру
     * @param currentAdapterPosition индекс базового элемента
     * @param startViewPosition      координата от которой добавляем элементы
     */
    private fun addItemsUpperAdapterPos(recycler: RecyclerView.Recycler, currentAdapterPosition: Int, startViewPosition: Int) {
        var currentAdapterPosition = currentAdapterPosition
        var startViewPosition = startViewPosition

        // Добавляем нужные элементы снизу
        while (++currentAdapterPosition < itemCount) {

            if (startViewPosition < height + mMaxElementsInStack * mDecoratedChildHeight) {
                val view = recycler.getViewForPosition(currentAdapterPosition)


                addView(view)
                measureChildWithMargins(view, 0, 0)

                val leftEdge = mLeftMargin
                val rightEdge = leftEdge + mDecoratedChildWidth
                val bottomEdge = mDecoratedChildHeight
                val topEdge = startViewPosition + mTopMargin

                layoutDecorated(view, leftEdge, topEdge, rightEdge, topEdge + bottomEdge)
                detachView(view)
                mViewCache.put(currentAdapterPosition, view)

                startViewPosition = topEdge + bottomEdge + mBottomMargin
            }
        }
    }

    /**
     * Добавить на экран из адаптера элементы ниже(больше индекс в адаптере) базового элемента
     *
     * @param recycler               RecyclerView.Recycler для доступа к адаптеру
     * @param currentAdapterPosition индекс базового элемента
     * @param startViewPosition      координата от которой добавляем элементы
     */
    private fun addItemsLowerAdapterPos(recycler: RecyclerView.Recycler, currentAdapterPosition: Int, startViewPosition: Int) {
        var currentAdapterPosition = currentAdapterPosition
        var startViewPosition = startViewPosition

        // Добавляем нужные элементы сверху
        while (--currentAdapterPosition >= 0 && currentAdapterPosition < itemCount) {

            if (startViewPosition > -mMaxElementsInStack * mDecoratedChildHeight) {
                val view = recycler.getViewForPosition(currentAdapterPosition)

                addView(view)
                measureChildWithMargins(view, 0, 0)

                val leftEdge = mLeftMargin
                val rightEdge = leftEdge + mDecoratedChildWidth
                val bottomEdge = startViewPosition - mBottomMargin
                val topEdge = bottomEdge - mTopMargin - mDecoratedChildHeight

                layoutDecorated(view, leftEdge, topEdge, rightEdge, bottomEdge)
                detachView(view)
                mViewCache.put(currentAdapterPosition, view)

                startViewPosition = topEdge + bottomEdge
            } else {
                break
            }
        }
    }

    /**
     * Раскрыть стеки - превращает текущий экран со стеками в сплошной список
     *
     * @param anchorView View
     */
    private fun expandStack(anchorView: View) {
        var baseItemDecoratedTop = getDecoratedTop(anchorView)

        // Расправляем верхний стек относительно якорной вьюхи
        for (i in mViewCache.indexOfValue(anchorView) - ONE_ELEMENT_OFFSET downTo 0) {
            val prevView = mViewCache.valueAt(i)
            val edge = baseItemDecoratedTop - mTopMargin
            val prevViewBottom = getDecoratedBottom(prevView) + mBottomMargin
            val offset = edge - prevViewBottom

            mViewCache.valueAt(i).offsetTopAndBottom(offset)
            baseItemDecoratedTop = getDecoratedTop(prevView)
        }

        var baseItemDecoratedBottom = getDecoratedBottom(anchorView)

        // Расправлем нижний стек
        for (i in mViewCache.indexOfValue(anchorView) + ONE_ELEMENT_OFFSET until mViewCache.size()) {
            val nextView = mViewCache.valueAt(i)
            val edge = baseItemDecoratedBottom + mBottomMargin
            val nextViewTop = getDecoratedTop(nextView) - mTopMargin
            val offset = edge - nextViewTop

            mViewCache.valueAt(i).offsetTopAndBottom(offset)
            baseItemDecoratedBottom = getDecoratedBottom(nextView)
        }
    }

    /**
     * Конвертация dp в px
     *
     * @param resources Resources
     * @param dp        dp
     * @return px
     */
    private fun convertDpToPx(resources: Resources, dp: Int): Int {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics))
    }

    internal enum class ScrollState {
        SCROLL_NA,
        SCROLL_DOWN_END,
        SCROLL_UP_END
    }

    companion object {

        private val ONE_ELEMENT_OFFSET = 1

        /**
         * Высота(величина) смещения элемента в стопке в dp
         */
        private val ITEM_HEIGHT_IN_STACK_IN_DP = 20

        /**
         * Относительная чать экрана вверху и внизу отдаваемая под стэк
         */
        private val RELATIVE_SCREEN_PART_TO_STACK = 6
    }
}