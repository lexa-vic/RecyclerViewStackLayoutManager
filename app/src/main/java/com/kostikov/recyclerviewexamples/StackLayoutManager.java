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

    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;

    private int mItemHeadInStackHeightInDp;

    private int mLeftMargin;
    private int mRightMargin;
    private int mTopMargin;
    private int mBottomMargin;

    private int mMaxElementsInStack = 0;
    private int mItemHeightInStackInPx;

    private SparseArray<View> viewCache = new SparseArray<View>();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        //We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            //Nothing to do during prelayout when empty
            return;
        }

        if (getChildCount() == 0) { //First or empty layout
            //Scrap measure one child
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);

            /*
             * We make some assumptions in this code based on every child
             * view being the same size (i.e. a uniform grid). This allows
             * us to compute the following values up front because they
             * won't change.
             */
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

        fillDown(0, recycler);
        //fillStack(recycler, 0, 0);
    }

    private void fillStack(RecyclerView.Recycler recycler, int startPosition, int startTopEdge){
        boolean isFillDownAvailable = true;
        int currentPosition = startPosition;
        int topEdge = startTopEdge;

        while (isFillDownAvailable && currentPosition < getItemCount()){

            View view = recycler.getViewForPosition(currentPosition);

            addView(view);
            measureChildWithMargins(view, 0, 0);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

            int leftEdge = getDecoratedLeft(view) + layoutParams.leftMargin;
            int rightEdge =  leftEdge + getDecoratedMeasuredWidth(view);
            int bottomEdge = getDecoratedMeasuredHeight(view);
            if (currentPosition == 0) topEdge += layoutParams.topMargin;

            currentPosition++;

            layoutDecorated(view, leftEdge, topEdge, rightEdge, topEdge + bottomEdge);

            if (topEdge >= getTopEdgeOfBottomStack() &&
                    topEdge < getHeight()){

                topEdge += mItemHeightInStackInPx;

            } else if(topEdge < getHeight()) {
                topEdge += bottomEdge + layoutParams.bottomMargin + layoutParams.topMargin;

                if (topEdge >= getTopEdgeOfBottomStack()) topEdge = getTopEdgeOfBottomStack();
            }
            else {
                isFillDownAvailable = false;
            }

        }

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

        viewCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);

            viewCache.put(position, view);
        }

        if (dy > 0)
        {
            fillDown(dy, recycler);
        } else {
            fillUp(dy, recycler);
        }

        return dy;
    }

    /**
     * Заполнение экрана при скролинге вверх - т.е. раскрываем низний стэк
     * @param dy смещенние
     * @param recycler RecyclerView.Recycler
     */
    private void fillDown(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta;
        int currentTopEdge;
        int futureTopEdge;
        int edgeLimit = 0;
        int finishEdge = 0;
        boolean fillFinish = false;

        currentPosition = viewCache.keyAt(0);

        detachAndScrapAttachedViews(recycler);

        while (currentPosition < getItemCount() && !fillFinish){

            // Берем вьюху из кэша detached вьюх
            View view = viewCache.get(currentPosition);

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){
                view = recycler.getViewForPosition(currentPosition);

                addView(view);
                measureChildWithMargins(view, 0, 0);

                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

                int leftEdge = layoutParams.leftMargin;
                int rightEdge =  leftEdge + mDecoratedChildWidth;
                int bottomEdge = mDecoratedChildHeight;


                if (viewCache.get(currentPosition - ONE_ELEMENT_OFFSET) != null){
                    int prevViewEdge = getDecoratedBottom(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET));

                    if (prevViewEdge >= getTopEdgeOfBottomStack()) {
                        currentTopEdge = getDecoratedTop(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;
                    } else {
                        currentTopEdge = prevViewEdge + layoutParams.bottomMargin + layoutParams.topMargin;
                    }
                } else {
                    // Если нет предыдущего, то это самый верхний элемент
                    currentTopEdge = layoutParams.topMargin;
                }

                if (currentTopEdge < getHeight()){
                    layoutDecorated(view, leftEdge, currentTopEdge, rightEdge, currentTopEdge + bottomEdge);
                    viewCache.put(currentPosition, view);
                } else {
                    fillFinish = true;
                    detachView(view);
                }
            } else {
                currentTopEdge = getDecoratedTop(view);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

                delta = -dy;
                futureTopEdge = currentTopEdge + delta;

                // Если становится виден полностью последний элемент
                if (viewCache.get(getItemCount() - ONE_ELEMENT_OFFSET) != null){
                    View lastView = viewCache.get(getItemCount() - ONE_ELEMENT_OFFSET);
                    int lastViewFutureTopEdge = getDecoratedTop(lastView) + delta;
                    int tmpEdgeLimit = getHeight() - mBottomMargin - mDecoratedChildHeight;
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

                    edgeLimit = getDecoratedBottom(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET))
                            + layoutParams.bottomMargin + layoutParams.topMargin;
                    // Нижняя граница предыдущего элемента заходит в стек - он вытягивается
                    if (edgeLimit >= getTopEdgeOfBottomStack()) {
                        int prevTopEdge = getDecoratedTop(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET));

                        // Если вверхняя граница предыдущего элемента за стеком, то тянемся к границе стека
                        if (prevTopEdge < getTopEdgeOfBottomStack()){
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

                            if (viewCache.indexOfValue(view) != 0){
                                edgeLimit = getDecoratedTop(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;
                            }
                        } else {
                            // Кол-во элементов в стеке больше максимального
                            // Надо подвинуть второй видимый элемент на первый и когда они сравняются удалить первый
                            edgeLimit = layoutParams.topMargin;

                            if (viewCache.indexOfValue(view) > 1) {
                                edgeLimit = getDecoratedTop(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) + mItemHeightInStackInPx;

                            }

                            if (viewCache.get(currentPosition - ONE_ELEMENT_OFFSET) != null &&
                                    getDecoratedTop(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET)) == currentTopEdge) {

                                detachView(viewCache.get(currentPosition - ONE_ELEMENT_OFFSET));
                                viewCache.remove(currentPosition - ONE_ELEMENT_OFFSET);
                            }
                        }
                    }
                }

                edgeLimit = Math.max(edgeLimit, finishEdge);

                // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                delta = futureTopEdge - edgeLimit > 0 ? -dy : edgeLimit - currentTopEdge;

                attachView(view);
                view.offsetTopAndBottom(delta);
            }
            currentPosition++;
        }
    }


    /**
     *
     * Скролл вверх - раскрываем верхний стек
     * @param dy смещенние
     * @param recycler RecyclerView.Recycler
     */
    private void fillUp(int dy, RecyclerView.Recycler recycler){
        int currentPosition;
        int delta;
        int currentTopEdge;
        int futureTopEdge;
        int edgeLimit;
        int finishEdge = getHeight();
        boolean fillFinish = false;

        currentPosition = viewCache.keyAt(viewCache.size() - 1);

        detachAndScrapAttachedViews(recycler);

        while (currentPosition >= 0 && currentPosition < getItemCount()){

            // Берем вьюху из кэша detached вьюх
            View view = viewCache.get(currentPosition);

            edgeLimit = getHeight() - mItemHeightInStackInPx;

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){

                if (viewCache.get(currentPosition + ONE_ELEMENT_OFFSET) != null &&
                        getDecoratedTop(viewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) > mTopMargin + 10){
                    view = recycler.getViewForPosition(currentPosition);

                    addView(view);
                    measureChildWithMargins(view, 0, 0);

                    int leftEdge = mLeftMargin;
                    int rightEdge =  leftEdge + mDecoratedChildWidth;
                    int bottomEdge = mDecoratedChildHeight;

                    // Если нет предыдущего, то это самый верхний элемент
                    currentTopEdge = mTopMargin;

                    layoutDecorated(view, leftEdge, currentTopEdge, rightEdge, currentTopEdge + bottomEdge);
                    //viewCache.put(currentPosition, view);

                    Log.d(TAG, String.format("addView view position %d top %d", currentPosition, view.getTop()));
                }

            } else {
                currentTopEdge = getDecoratedTop(view);

                delta = -dy;
                futureTopEdge = currentTopEdge + delta;

                // Если
                if (viewCache.get(0) != null){
                    View secondView = viewCache.get(1);
                    int secondViewFutureTopEdge = getDecoratedTop(secondView) + delta;
                    int tmpEdgeLimit = getDecoratedBottom(viewCache.get(0)) + mTopMargin + mBottomMargin;
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
                        && viewCache.get(currentPosition + ONE_ELEMENT_OFFSET) != null
                        && getDecoratedTop(viewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) <= getTopEdgeOfBottomStack()){
                    // Элемент можеть уже выйти за экран и быть уничтожен

                    int currentNextTopEdge = getDecoratedTop(viewCache.get(currentPosition + ONE_ELEMENT_OFFSET));
                    int currentBottomEdge = getDecoratedBottom(viewCache.get(currentPosition)) + mTopMargin + mBottomMargin;

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

                            if (viewCache.indexOfValue(view) != viewCache.size() - 1){
                                edgeLimit = getDecoratedTop(viewCache.get(currentPosition + ONE_ELEMENT_OFFSET)) - mItemHeightInStackInPx;
                            }
                        } else {
                            // Кол-во элементов в стеке больше максимального
                            // Надо подвинуть предпоследний видимый элемент на последний и когда они сравняются удалить последний
                            edgeLimit = getHeight() - mItemHeightInStackInPx;


                            if (viewCache.indexOfValue(view) == viewCache.size() - 1) {
                                edgeLimit = getHeight();
                            }

                            if (futureTopEdge >= getHeight()){
                                //removeAndRecycleView(viewCache.get(currentPosition), recycler);
                                detachView(viewCache.get(currentPosition));
                                Log.d(TAG, String.format("detachView view position %d", currentPosition + ONE_ELEMENT_OFFSET));
                                viewCache.remove(currentPosition);
                                currentPosition--;
                                continue;
                            }
                        }
                    }
                }

                edgeLimit = Math.min(edgeLimit, finishEdge);

                // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                delta = edgeLimit - futureTopEdge > 0 ? -dy : edgeLimit - currentTopEdge;

                Log.d(TAG, String.format("Attach view position %d %d", currentPosition, delta));
                view.offsetTopAndBottom(delta);
            }
            currentPosition--;
        }

        for (int i = 0; i < viewCache.size(); i++){

            View view = viewCache.valueAt(i);
            attachView(view);
        }
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

        for (int i = 0; i < viewCache.size(); i++){
            View view = viewCache.valueAt(i);

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

        for (int i = 0; i < viewCache.size(); i++){
            View view = viewCache.valueAt(i);

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
}
