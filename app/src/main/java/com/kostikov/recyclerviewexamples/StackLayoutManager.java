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
public class StackLayoutManager extends RecyclerView.LayoutManager {

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

        mViewCache.clear();
        mRemoveCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);
            mViewCache.put(position, view);
        }

        for (int i = 0; i < mViewCache.size(); i++){
            detachView(mViewCache.valueAt(i));
        }

        fillDown(0, recycler);


        for (int i = 0; i < mRemoveCache.size(); i++){
            recycler.recycleView(mRemoveCache.valueAt(i));
        }

        mScrollState = ScrollState.SCROLL_NA;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta;

        if (getChildCount() == 0 && dy == 0) {
            return 0;
        }

        if ((dy > 0 && mScrollState == ScrollState.SCROLL_DOWN_END)
                || (dy < 0 && mScrollState == ScrollState.SCROLL_UP_END)) {
            return 0;
        }


        mViewCache.clear();
        mRemoveCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);
            mViewCache.put(position, view);
        }

        for (int i = 0; i < mViewCache.size(); i++){
            detachView(mViewCache.valueAt(i));

            Log.d(TAG, String.format("detachView %d ", mViewCache.keyAt(i) ));
        }

        if (dy > 0)
        {
            delta = fillDown(dy, recycler);
            mScrollState = delta == 0 ? ScrollState.SCROLL_DOWN_END : ScrollState.SCROLL_NA;
        } else {
            delta = fillUp(dy, recycler);
            mScrollState = delta == 0 ? ScrollState.SCROLL_UP_END : ScrollState.SCROLL_NA;
        }

        for (int i = 0; i < mRemoveCache.size(); i++){
            recycler.recycleView(mRemoveCache.valueAt(i));
        }

        return delta != 0 ? dy : 0;
    }

    /**
     * Заполнение экрана при скролинге вверх - т.е. раскрываем низний стэк
     * @param dy смещенние
     * @param recycler RecyclerView.Recycler
     */
    private int fillDown(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta;
        int returnDelta = 0;
        int currentTopEdge;
        int futureTopEdge;
        int edgeLimit = 0;
        int finishEdge = 0;
        boolean fillFinish = false;

        currentPosition = mViewCache.keyAt(0);

        while (currentPosition < getItemCount()){

            // Берем вьюху из кэша detached вьюх
            View view = mViewCache.get(currentPosition);

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){
                view = recycler.getViewForPosition(currentPosition);

                addView(view);
                measureChildWithMargins(view, 0, 0);

                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

                int leftEdge = layoutParams.leftMargin;
                int rightEdge =  leftEdge + mDecoratedChildWidth;
                int bottomEdge = mDecoratedChildHeight;


                if (mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET) != null){
                    int prevViewEdge = getDecoratedBottom(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET));
                    // Нижний край заезжает за нижний стек надо складывать остальные в стек
                    if (prevViewEdge >= getTopEdgeOfBottomStack()) {
                        int bottomItems = getItemCount() - currentPosition;

                        // Если элементов меньше чем максимальное кол-во в стеке, то они прижаты к низу
                        if (bottomItems < mMaxElementsInStack) {
                            // Кроме первого который на границе стека
                            if (bottomEdge == mMaxElementsInStack - ONE_ELEMENT_OFFSET){
                                currentTopEdge = getTopEdgeOfBottomStack();
                            } else {
                                currentTopEdge = getHeight() - mItemHeightInStackInPx * bottomItems;
                            }
                        } else {
                            currentTopEdge = getDecoratedTop(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;
                        }
                    } else {
                        currentTopEdge = prevViewEdge + layoutParams.bottomMargin + layoutParams.topMargin;
                    }
                } else {
                    // Если нет предыдущего, то это самый верхний элемент
                    currentTopEdge = layoutParams.topMargin;
                }

                if (currentTopEdge < getHeight()){
                    layoutDecorated(view, leftEdge, currentTopEdge, rightEdge, currentTopEdge + bottomEdge);
                    mViewCache.put(currentPosition, view);
                } else {
                    detachView(view);
                    break;
                }
            } else {
                currentTopEdge = getDecoratedTop(view);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

                delta = -dy;
                futureTopEdge = currentTopEdge + delta;

                // Если становится виден полностью последний элемент
                if (mViewCache.get(getItemCount() - ONE_ELEMENT_OFFSET) != null){
                    View lastView = mViewCache.get(getItemCount() - ONE_ELEMENT_OFFSET);
                    int lastViewFutureTopEdge = getDecoratedTop(lastView) + delta;
                    int tmpEdgeLimit = getHeight() - mBottomMargin - mDecoratedChildHeight;
                    fillFinish = true;
                    // После скрола должен полностью быть виден
                    if (lastViewFutureTopEdge <= tmpEdgeLimit) {
                        // Вычисляем максимальное смещение которое может быть для последнего элемента
                        int finishOffset = lastViewFutureTopEdge - tmpEdgeLimit > 0 ? -dy : tmpEdgeLimit - getDecoratedTop(lastView);
                        // Устанавливаем финальную границу эелементов
                        finishEdge = currentTopEdge + finishOffset;
                    }
                }

                // Элементы нижнего стека
                if (currentTopEdge >= getTopEdgeOfBottomStack()){

                    edgeLimit = getDecoratedBottom(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET))
                            + layoutParams.bottomMargin + layoutParams.topMargin;
                    // Нижняя граница предыдущего элемента заходит в стек - он вытягивается
                    if (edgeLimit >= currentTopEdge) {
                        int prevTopEdge = getDecoratedTop(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET));

                        if (fillFinish) {
                            edgeLimit = currentTopEdge;
                        } else if (prevTopEdge < getTopEdgeOfBottomStack()){ // Если вверхняя граница предыдущего элемента за стеком, то тянемся к границе стека
                            edgeLimit = getTopEdgeOfBottomStack();
                        } else {
                            // Если предудщий ниже границы стека то соблюдаем отступ до него
                            edgeLimit = prevTopEdge + mItemHeightInStackInPx;
                        }
                    }
                    // Элементы выше нижнего стека
                } else {
                    // Если будущее положение элемента после скрола заезжает на верхний стек
                    if (futureTopEdge < getBottomEdgeOfTopStack()){
                        // Кол-во элементов в стеке меньше максимального
                        if (getTopStackSize() <= mMaxElementsInStack){

                            edgeLimit = layoutParams.topMargin;

                            if (mViewCache.indexOfValue(view) != 0){
                                edgeLimit = getDecoratedTop(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;
                            }
                        } else {
                            // Кол-во элементов в стеке больше максимального
                            // Надо подвинуть второй видимый элемент на первый и когда они сравняются удалить первый
                            edgeLimit = layoutParams.topMargin;

                            if (mViewCache.indexOfValue(view) > 1) {
                                edgeLimit = getDecoratedTop(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;
                            }

                            if (mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET) != null &&
                                    getDecoratedTop(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) == currentTopEdge) {

                                detachView(mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET));

                                mRemoveCache.put(currentPosition, mViewCache.get(currentPosition - ONE_ELEMENT_OFFSET));
                                mViewCache.remove(currentPosition - ONE_ELEMENT_OFFSET);
                            }
                        }
                    }
                }

                edgeLimit = Math.max(edgeLimit, finishEdge);

                // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                delta = futureTopEdge - edgeLimit > 0 ? -dy : edgeLimit - currentTopEdge;

                attachView(view);
                view.offsetTopAndBottom(delta);

                returnDelta = Math.min(delta, returnDelta);
            }
            currentPosition++;
        }

        return returnDelta;
    }


    /**
     *
     * Скролл вверх - раскрываем верхний стек
     * @param dy смещенние
     * @param recycler RecyclerView.Recycler
     */
    private int fillUp(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta;
        int returnDelta = 0;
        int currentTopEdge;
        int futureTopEdge;
        int edgeLimit;
        int finishEdge = getHeight();
        boolean fillFinish = false;

        currentPosition = mViewCache.keyAt(mViewCache.size() - 1);

        while (currentPosition >= 0 && currentPosition < getItemCount()){

            // Берем вьюху из кэша detached вьюх
            View view = mViewCache.get(currentPosition);

            edgeLimit = getHeight() - mItemHeightInStackInPx;

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){

                if (mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET) != null &&
                        getDecoratedTop(mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) > mTopMargin + 10){
                    view = recycler.getViewForPosition(currentPosition);

                    addView(view);
                    measureChildWithMargins(view, 0, 0);

                    int leftEdge = mLeftMargin;
                    int rightEdge =  leftEdge + mDecoratedChildWidth;
                    int bottomEdge = mDecoratedChildHeight;

                    // Если нет предыдущего, то это самый верхний элемент
                    currentTopEdge = mTopMargin;

                    layoutDecorated(view, leftEdge, currentTopEdge, rightEdge, currentTopEdge + bottomEdge);

                    Log.d(TAG, String.format("addView %d ", currentPosition ));

                    mViewCache.put(getPosition(view), view);
                    detachView(view);
                }

            } else {
                currentTopEdge = getDecoratedTop(view);

                delta = -dy;
                futureTopEdge = currentTopEdge + delta;

                // Если
                if (mViewCache.get(0) != null){
                    View secondView = mViewCache.get(1);
                    int secondViewFutureTopEdge = getDecoratedTop(secondView) + delta;
                    int tmpEdgeLimit = getDecoratedBottom(mViewCache.get(0)) + mTopMargin + mBottomMargin;
                    fillFinish = true;
                    // После скрола должен полностью быть виден
                    if (secondViewFutureTopEdge >= tmpEdgeLimit) {
                        // Вычисляем максимальное смещение которое может быть других элементов кроме первого
                        int finishOffset = tmpEdgeLimit - secondViewFutureTopEdge > 0 ? -dy : tmpEdgeLimit - getDecoratedTop(secondView);
                        // Устанавливаем финальную границу эелементов
                        finishEdge = currentTopEdge + finishOffset;
                    }
                }

                // Элементы верхнего стека
                if (currentTopEdge <= getBottomEdgeOfTopStack()
                        && mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET) != null
                        && getDecoratedTop(mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) <= getTopEdgeOfBottomStack()){
                    // Элемент можеть уже выйти за экран и быть уничтожен

                    int currentNextTopEdge = getDecoratedTop(mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET));
                    int currentBottomEdge = getDecoratedBottom(mViewCache.get(currentPosition)) + mTopMargin + mBottomMargin;

                    Log.d(TAG, String.format("currentTopEdge currentPos %d", currentPosition));

                    if (currentNextTopEdge >= currentBottomEdge){
                        edgeLimit = currentNextTopEdge - mTopMargin - mBottomMargin - mDecoratedChildHeight;
                    } else {

                        if (fillFinish){
                            edgeLimit = currentTopEdge;
                        } else  if (currentNextTopEdge > getBottomEdgeOfTopStack()){
                            edgeLimit = getBottomEdgeOfTopStack();
                        }
                        else {
                            if (currentPosition == 0){
                                edgeLimit = mTopMargin;
                            } else {
                                edgeLimit = currentNextTopEdge - mItemHeightInStackInPx;
                                edgeLimit = edgeLimit <= mTopMargin ? mTopMargin : edgeLimit;
                            }
                        }
                    }

                    // Элементы ниже верхнего стека
                } else {

                    // Если будущее положение элемента после скрола заезжает на нижний стек
                    if (futureTopEdge + mDecoratedChildHeight > getTopEdgeOfBottomStack()){
                        // Кол-во элементов в стеке меньше максимального
                        if (getBottomStackSize() < mMaxElementsInStack){

                            edgeLimit = getHeight() - mItemHeightInStackInPx;

                            Log.d(TAG, String.format("mViewCache.size %d currentPos %d", mViewCache.size(), currentPosition));

                            if (mViewCache.indexOfValue(view) != mViewCache.size() - 1){

                                edgeLimit = getDecoratedTop(mViewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) - mItemHeightInStackInPx;
                            }
                        } else {
                            // Кол-во элементов в стеке больше максимального
                            // Надо подвинуть предпоследний видимый элемент на последний и когда они сравняются удалить последний
                            edgeLimit = getHeight() - mItemHeightInStackInPx;


                            if (mViewCache.indexOfValue(view) == mViewCache.size() - 1) {
                                edgeLimit = getHeight();
                            }

                            if (futureTopEdge >= getHeight()){
                                mRemoveCache.put(currentPosition, mViewCache.get(currentPosition));
                                mViewCache.remove(currentPosition);
                                Log.d(TAG, String.format("mViewCache.remove %d ", currentPosition ));
                                currentPosition--;
                                continue;
                            }
                        }
                    }
                }

                edgeLimit = Math.min(edgeLimit, finishEdge);

                // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                delta = edgeLimit - futureTopEdge > 0 ? -dy : edgeLimit - currentTopEdge;

                returnDelta = Math.max(delta, returnDelta);
                view.offsetTopAndBottom(delta);
            }
            currentPosition--;
        }

        for (int i = 0; i < mViewCache.size(); i++){

            View view = mViewCache.valueAt(i);
            attachView(view);
        }

        return returnDelta;
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
