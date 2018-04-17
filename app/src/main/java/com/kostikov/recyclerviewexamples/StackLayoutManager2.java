package com.kostikov.recyclerviewexamples;


import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;


/**
 * @author Kostikov Aleksey.
 */
public class StackLayoutManager2 extends RecyclerView.LayoutManager {

    private static final String TAG = StackLayoutManager.class.getName();

    private static final int ONE_ELEMENT_OFFSET = 1;

    /**
     * Высота(величина) смещения элемента в стопке в dp
     */
    private static final int ITEM_HEIGHT_IN_STACK_IN_DP = 20;

    /**
     * Относительная чать экрана вверху и внизу отдаваемая под стэк
     */
    private static final int RELATIVE_SCREEN_PART_TO_STACK = 6;

    /* Высота и ширина элементов */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;

    private int mLeftMargin;
    private int mRightMargin;
    private int mTopMargin;
    private int mBottomMargin;

    private int mMaxElementsInStack = 0;
    private int mItemHeightInStackInPx;

    private SparseArray<View> mViewCache = new SparseArray<View>();
    private SparseArray<View> mRemoveCache = new SparseArray<View>();

    private ScrollState mScrollState = ScrollState.SCROLL_NA;


    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        if (getChildCount() == 0) {
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);

            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);

            mItemHeightInStackInPx = convertDpToPx(scrap.getResources(), ITEM_HEIGHT_IN_STACK_IN_DP);
            mMaxElementsInStack = getBottomEdgeOfTopStack() / mItemHeightInStackInPx;

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)scrap.getLayoutParams();
            mLeftMargin = layoutParams.leftMargin;
            mRightMargin = layoutParams.rightMargin;
            mTopMargin = layoutParams.topMargin;
            mBottomMargin = layoutParams.bottomMargin;

            detachAndScrapView(scrap, recycler);
        }

        // Делаем detach всех view на экране, помещаем в Scrap
        detachAndScrapAttachedViews(recycler);

        fillViewCache();

        if (mViewCache.size() == 0) {
            int startViewPosition = 0;
            int startAdapterPosition = -1;
            addItemsUpperAdapterPos(recycler, startAdapterPosition, startViewPosition);
            createBottomStackScrollDown();
        }

        attachViewCache();

        mScrollState = ScrollState.SCROLL_NA;
    }

    private void fillViewCache() {
        mViewCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);
            mViewCache.put(position, view);
        }

        for (int i = 0; i < mViewCache.size(); i++){
            detachView(mViewCache.valueAt(i));
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = dy;

        if (getChildCount() == 0 && dy == 0) {
            return 0;
        }

        if ((dy > 0 && mScrollState == ScrollState.SCROLL_DOWN_END)
                || (dy < 0 && mScrollState == ScrollState.SCROLL_UP_END)) {
            return 0;
        }

        fillViewCache();

        if (dy > 0)
        {
            delta = scrollDown(dy, recycler);
            mScrollState = delta == 0 ? ScrollState.SCROLL_DOWN_END : ScrollState.SCROLL_NA;
        } else {
            delta = scrollUp(dy, recycler);
            mScrollState = delta == 0 ? ScrollState.SCROLL_UP_END : ScrollState.SCROLL_NA;
        }

        attachViewCache();


        return dy;
    }

    private void attachViewCache() {
        for (int i = 0; i < mViewCache.size(); i++){
            attachView(mViewCache.valueAt(i));
        }
    }

    private int scrollDown(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta = -dy;

        View anchorView = getAnchorView();

        if (anchorView == null) return 0;

        // Раскрываем стеки превращаем в обычный список
        expandStack(anchorView);

        currentPosition = mViewCache.keyAt(mViewCache.size() - 1);

        // Появился последний элемент, урезаем delta до момента когда полностью виден последний элемент
        if (mViewCache.keyAt(mViewCache.size() - 1) == getItemCount() - 1){
            int lastItemBottomEdge = getDecoratedBottom(mViewCache.get(getItemCount() - 1)) + mBottomMargin;
            int futureEdge = lastItemBottomEdge + delta;
            delta = futureEdge - getHeight() >= 0 ? delta : getHeight() - lastItemBottomEdge;
        }

        // Скролим
        for (int i = 0; i < mViewCache.size(); i++){
            mViewCache.valueAt(i).offsetTopAndBottom(delta);
        }

        View view = mViewCache.get(currentPosition);
        int baseItemDecoratedBottom = getDecoratedBottom(view);// + mBottomMargin;

        // Добавляем необходимые элементы после скрола
        addItemsUpperAdapterPos(recycler, currentPosition, baseItemDecoratedBottom);
        // Сворачиваем в верхний стек
        createTopStackScrollDown(recycler);
        // Сворачиваем в нижний стек
        createBottomStackScrollDown();

        return delta;
    }

    private int scrollUp(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta = -dy;

        View anchorView = getAnchorView();

        if (anchorView == null) return 0;

        // Раскрываем стеки превращаем в обычный список
        expandStack(anchorView);

        currentPosition = mViewCache.keyAt(0);


        // Появился первый элемент, урезаем delta до момента когда полностью виден последний элемент
        if (mViewCache.get(0) != null){
            int firstItemTopEdge = getDecoratedTop(mViewCache.get(0));
            int futureEdge = firstItemTopEdge + delta;
            delta = mTopMargin - futureEdge >= 0 ? delta : mTopMargin - firstItemTopEdge;
        }

        // Скролим
        for (int i = 0; i < mViewCache.size(); i++){
            mViewCache.valueAt(i).offsetTopAndBottom(delta);
        }

        View view = mViewCache.get(currentPosition);
        int baseItemDecoratedTop = getDecoratedTop(view);

        // Добавляем необходимые элементы после скрола
        addItemsLowerAdapterPos(recycler, currentPosition, baseItemDecoratedTop);

        createTopStackScrollUp();

        return delta;
    }

    private void createTopStackScrollUp(){
        int edgeLimit = mTopMargin;
        int startPos = 0;
        int topStackSize = getTopStackSize();

        if (topStackSize > mMaxElementsInStack) {
            startPos = topStackSize - mMaxElementsInStack;
        }

        for (int i = startPos; i >= 0 ; i--){
            edgeLimit = mTopMargin;
            int offset = edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0 ? edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) : 0;
            mViewCache.valueAt(i).offsetTopAndBottom(offset);
        }

        Log.d(TAG, String.format("startPos %d", startPos));

        // Устанаваливаем первый элемент стека
        /*int offset = edgeLimit - getDecoratedTop(mViewCache.valueAt(startPos)) >= 0 ? edgeLimit - getDecoratedTop(mViewCache.valueAt(startPos)) : 0;
        mViewCache.valueAt(startPos).offsetTopAndBottom(offset);*/
        View anchorView =  mViewCache.valueAt(topStackSize);
        int topEdge = getDecoratedTop(anchorView);

        edgeLimit = mTopMargin + (Math.min(topStackSize, mMaxElementsInStack) - 1) * mItemHeightInStackInPx;
        edgeLimit = topEdge - edgeLimit > mItemHeightInStackInPx ? edgeLimit : topEdge - mItemHeightInStackInPx;

        Log.d(TAG, String.format("topStackSize %d", topStackSize));

        for (int i = topStackSize - 1; i >= startPos; i--)
        {
            int offset = edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0 ? edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) : 0;
            mViewCache.valueAt(i).offsetTopAndBottom(offset);

            if (topStackSize <= mMaxElementsInStack) {
                edgeLimit = mTopMargin + (i - 1) * mItemHeightInStackInPx;
            } else {
                edgeLimit = getDecoratedTop(mViewCache.valueAt(i)) - mItemHeightInStackInPx;
            }

        }

    }

    private void createBottomStackScrollDown() {
        View anchorView;
        int edgeLimit;
        int anchorPos;

        // Cворачиваем в нижний стек
        // Первый элемент чем верхний край выходит за нижний стек, он вытягивается и как только вытянулся начинают ехать другие элементы за ним
        anchorView = getFirstViewInBottomStack();

        if (anchorView != null) {

            anchorPos = getPosition(anchorView);

            // Элементы лежащие в видимом стеке
            SparseArray<View> stack = new SparseArray<>();
            int prevItemBottom = getDecoratedBottom(anchorView) + mBottomMargin + mTopMargin;

            for (int i = anchorPos + ONE_ELEMENT_OFFSET; i <= anchorPos + mMaxElementsInStack  && i < getItemCount(); i++) {
                stack.put(i, mViewCache.get(i));
            }

            edgeLimit = getHeight() - mItemHeightInStackInPx * (stack.size());
            // Верхний элемент в стеке не может быть ближе к anchorView чем mItemHeightInStackInPx
            edgeLimit = edgeLimit - getDecoratedTop(anchorView) > mItemHeightInStackInPx ?
                    edgeLimit : getDecoratedTop(anchorView) + mItemHeightInStackInPx;
            int bottomStackSize = getBottomStackSize();

            for (int i = 0; i < stack.size(); i++){

                int currentTop = getDecoratedTop(stack.valueAt(i));

                int offset = edgeLimit - prevItemBottom >= 0 ? 0 : edgeLimit - currentTop;
                stack.valueAt(i).offsetTopAndBottom(offset);

                if (bottomStackSize < mMaxElementsInStack){
                    // Если стек заканчивается, то элементы не тянем на верх
                    edgeLimit = getHeight() - mItemHeightInStackInPx * (stack.size() - i - ONE_ELEMENT_OFFSET);
                } else {
                    edgeLimit = getDecoratedTop(stack.valueAt(i)) + mItemHeightInStackInPx;
                }
                prevItemBottom = getDecoratedBottom(stack.valueAt(i));
            }
        }
    }

    private void createTopStackScrollDown(RecyclerView.Recycler recycler) {
        View anchorView;
        int edgeLimit;
        anchorView = getAnchorView();
        int anchorPos = getPosition(anchorView);

        if (anchorPos > mMaxElementsInStack) {

            edgeLimit = mTopMargin;

            int firstPosInStack = anchorPos - mMaxElementsInStack - 1;
            //Log.d(TAG, String.format("firstPosInStack %d ,anchorPos %d", firstPosInStack, anchorPos));
            int offset = edgeLimit - getDecoratedTop(mViewCache.get(firstPosInStack));
            // Первый элемент оставляем на своем месте на него будут заезжать другие карты
            mViewCache.get(firstPosInStack).offsetTopAndBottom(offset);

            // Последний элемент стека оставляем на месте или двигаем если сместился выше других вкладок в стеке
            edgeLimit = mTopMargin + mItemHeightInStackInPx * (mMaxElementsInStack - ONE_ELEMENT_OFFSET) ;
            offset = edgeLimit - getDecoratedTop(mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET)) >= 0
                    ? edgeLimit - getDecoratedTop(mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET)) : 0;
            mViewCache.get(anchorPos - ONE_ELEMENT_OFFSET).offsetTopAndBottom(offset);

            // Смещаем другие карты на первую относительно последней в стеке
            for (int i = anchorPos - 2; i > firstPosInStack; i--){
                edgeLimit = getDecoratedTop(mViewCache.get(i + ONE_ELEMENT_OFFSET)) - mItemHeightInStackInPx;
                offset = edgeLimit - getDecoratedTop(mViewCache.get(i)) >= 0 ? edgeLimit - getDecoratedTop(mViewCache.get(i)) : 0;
                mViewCache.get(i).offsetTopAndBottom(offset);
            }

            for (int i = 0; i < firstPosInStack; i++){

                if (mViewCache.get(i) != null){
                    recycler.recycleView(mViewCache.get(i));
                    mViewCache.remove(i);
                    Log.d(TAG, String.format("recycleView %d ", i));
                }
            }
        } else {
            edgeLimit = mTopMargin;
            // Складываем в стек
            for (int i = 0; i < getTopStackSize(); i++){

                int offset = edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) >= 0 ? edgeLimit - getDecoratedTop(mViewCache.valueAt(i)) : 0;
                mViewCache.valueAt(i).offsetTopAndBottom(offset);
                edgeLimit = getDecoratedTop(mViewCache.valueAt(i)) + mItemHeightInStackInPx;
            }
        }
    }

    private void addItemsUpperAdapterPos(RecyclerView.Recycler recycler, int currentAdapterPosition, int startViewPosition) {

        // Добавляем нужные элементы снизу
        while (++currentAdapterPosition < getItemCount()){

            if (startViewPosition < getHeight() + mMaxElementsInStack * mDecoratedChildHeight) {
                View view = recycler.getViewForPosition(currentAdapterPosition);

                addView(view);
                measureChildWithMargins(view, 0, 0);

                int leftEdge = mLeftMargin;
                int rightEdge =  leftEdge + mDecoratedChildWidth;
                int bottomEdge = mDecoratedChildHeight;
                int topEdge = startViewPosition + mTopMargin;

                layoutDecorated(view, leftEdge, topEdge, rightEdge, topEdge + bottomEdge);
                detachView(view);
                mViewCache.put(currentAdapterPosition, view);

                startViewPosition = topEdge + bottomEdge + mBottomMargin;
            }
        }
    }

    private void addItemsLowerAdapterPos(RecyclerView.Recycler recycler, int currentAdapterPosition, int startViewPosition) {

        // Добавляем нужные элементы сверху
        while (--currentAdapterPosition >= 0 && currentAdapterPosition < getItemCount()){

            if (startViewPosition > - (mMaxElementsInStack - 1) * mDecoratedChildHeight) {
                View view = recycler.getViewForPosition(currentAdapterPosition);

                Log.d(TAG, String.format("add view %d", currentAdapterPosition));

                addView(view);
                measureChildWithMargins(view, 0, 0);

                int leftEdge = mLeftMargin;
                int rightEdge =  leftEdge + mDecoratedChildWidth;
                int bottomEdge = startViewPosition - mBottomMargin;
                int topEdge = bottomEdge - mTopMargin - mDecoratedChildHeight;

                layoutDecorated(view, leftEdge, topEdge, rightEdge, bottomEdge);
                detachView(view);
                mViewCache.put(currentAdapterPosition, view);

                startViewPosition = topEdge + bottomEdge;
            } else {
                break;
            }
        }
    }

    private void expandStack(View anchorView) {
        int baseItemDecoratedTop = getDecoratedTop(anchorView);

        // Расправляем верхний стек относительно якорной вьюхи
        for (int i = mViewCache.indexOfValue(anchorView) - ONE_ELEMENT_OFFSET; i >= 0 ; i--){
            View prevView = mViewCache.valueAt(i);
            int edge = baseItemDecoratedTop - mTopMargin;
            int prevViewBottom = getDecoratedBottom(prevView) + mBottomMargin;
            int offset = edge - prevViewBottom;

            mViewCache.valueAt(i).offsetTopAndBottom(offset);
            baseItemDecoratedTop = getDecoratedTop(prevView);
        }

        int baseItemDecoratedBottom = getDecoratedBottom(anchorView);

        // Расправлем нижний стек
        for (int i = mViewCache.indexOfValue(anchorView) + ONE_ELEMENT_OFFSET; i < mViewCache.size(); i++){
            View nextView = mViewCache.valueAt(i);
            int edge = baseItemDecoratedBottom + mBottomMargin;
            int nextViewTop = getDecoratedTop(nextView) - mTopMargin;
            int offset = edge - nextViewTop;

            mViewCache.valueAt(i).offsetTopAndBottom(offset);
            baseItemDecoratedBottom = getDecoratedBottom(nextView);
        }
    }

    private View getAnchorView(){
        View view = null;

        for (int i = 0; i < mViewCache.size(); i++){
            if (getDecoratedTop(mViewCache.valueAt(i)) >= getBottomEdgeOfTopStack()){
                view = mViewCache.valueAt(i);
                break;
            }
        }
        return view;
    }

    private View getFirstViewInBottomStack(){
        View view = null;

        for (int i = mViewCache.size() - ONE_ELEMENT_OFFSET; i >= 0; i-- ){
            if (getDecoratedTop(mViewCache.valueAt(i)) <= getTopEdgeOfBottomStack()){
                view = mViewCache.valueAt(i);
                break;
            }
        }
        return view;
    }

    /**
     * Возвращает нижнюю гранизу нижнего стека
     * Расчитывается относительно высоты родителя
     *
     * @return int граница стека.
     */
    private int getBottomEdgeOfTopStack(){
        return getHeight()/RELATIVE_SCREEN_PART_TO_STACK;
    }

    /**
     * Возвращает текущее количество элементов в верхнем стеке
     *
     * @return int кол-во элементов в стеке
     */
    private int getTopStackSize(){
        int cnt = 0;

        for (int i = 0; i < mViewCache.size(); i++){
            View view = mViewCache.valueAt(i);

            if (view.getTop() < getBottomEdgeOfTopStack()){
                cnt++;
            }
        }

        return cnt;
    }

    /**
     * Возвращает текущее количество элементов в верхнем стеке
     *
     * @return int кол-во элементов в стеке
     */
    private int getBottomStackSize(){
        int cnt = 0;

        for (int i = 0; i < mViewCache.size(); i++){
            View view = mViewCache.valueAt(i);

            if (view.getTop() >= getTopEdgeOfBottomStack()){
                cnt++;
            }
        }

        return cnt;
    }

    /**
     * Возвращает верхнуюю гранизу нижнего стека
     * Расчитывается относительно высоты родителя
     *
     * @return int граница стека.
     */
    private int getTopEdgeOfBottomStack(){
        return getHeight() - getHeight()/RELATIVE_SCREEN_PART_TO_STACK;
    }

    /**
     * Конвертация dp в px
     *
     * @param resources Resources
     * @param dp dp
     * @return px
     */
    private int convertDpToPx(@NonNull Resources resources, int dp){
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
    }

    private enum ScrollState {
        SCROLL_NA,
        SCROLL_DOWN_END,
        SCROLL_UP_END;
    }
}
