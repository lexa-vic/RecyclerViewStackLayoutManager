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

    private int mMaxElementsInStack = 0;

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



            mMaxElementsInStack = getBottomEdgeOfTopStack() / convertDpToPx(scrap.getResources(), ITEM_HEIGHT_IN_STACK_IN_DP);

            detachAndScrapView(scrap, recycler);

        }


        // Делаем detach всех view на экране, помещаем в Scrap
        detachAndScrapAttachedViews(recycler);


        fillStack(recycler);
    }

    private void fillStack(RecyclerView.Recycler recycler){
        boolean isFillDownAvailable = true;
        int currentPosition = 0;
        int topEdge = 0;

        viewCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);
            viewCache.put(position, view);
        }

        for (int i = 0; i < viewCache.size(); i++){
            detachView(viewCache.valueAt(i));
        }

        while (isFillDownAvailable && currentPosition < getItemCount()){
            // Берем вьюху из кэша detached вьюх
            View view = viewCache.get(currentPosition);

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){
                view = recycler.getViewForPosition(currentPosition);


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

                    topEdge += convertDpToPx(view.getResources(), ITEM_HEIGHT_IN_STACK_IN_DP);

                } else if(topEdge < getHeight()) {
                    topEdge += bottomEdge + layoutParams.bottomMargin + layoutParams.topMargin;

                    if (topEdge >= getTopEdgeOfBottomStack()) topEdge = getTopEdgeOfBottomStack();
                }
                else {
                    isFillDownAvailable = false;
                }

            } else {
                attachView(view);
                viewCache.remove(currentPosition);
            }
        }

        /*
         * Finally, we ask the Recycler to scrap and store any views
         * that we did not re-attach. These are views that are not currently
         * necessary because they are no longer visible.
         */
        for (int i=0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            recycler.recycleView(removingView);
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.d(TAG, String.format("Scroll %d", dy));

        if (getChildCount() == 0) {
            return 0;
        }

        if (dy > 0)
        {
            fillUp(dy, recycler);
        }
        //offsetChildrenVertical(-dy);

        return dy;
    }

    /**
     * Заполнение экрана при скролинге вверх - т.е. раскрываем низний стэк
     * @param dy
     * @param recycler
     */
    private void fillUp(int dy, RecyclerView.Recycler recycler){
        int currentPosition = 0;
        int delta = -dy;
        int currentTopEdge = 0;
        int futureTopEdge = 0;

        SparseArray<View> removeViewCache = new SparseArray<View>();

        viewCache.clear();

        for (int i = 0; i < getChildCount(); i++){
            View view = getChildAt(i);
            int position = getPosition(view);

            viewCache.put(position, view);
        }


        currentPosition = viewCache.keyAt(0);

        for (int i = 0; i < viewCache.size(); i++){
            //detachView(viewCache.valueAt(i));
            detachAndScrapView(viewCache.valueAt(i), recycler);
        }

        while (currentPosition < getItemCount()){

            // Берем вьюху из кэша detached вьюх
            View view = viewCache.get(currentPosition);

            // Если нет вьюхи по этой позиции то достаем из ресайклера
            if (view == null){
               break;
            } else {
                //attachView(view);
                //detachAndScrapView(view, recycler);

            }


            currentTopEdge = getDecoratedTop(view);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)view.getLayoutParams();

            delta = -dy;
            futureTopEdge = currentTopEdge + delta;

            // Элементы нижнего стека
             if (currentTopEdge >= getTopEdgeOfBottomStack()){

                 int edgeLimit = getDecoratedBottom(viewCache.get(currentPosition - 1)) + layoutParams.bottomMargin + layoutParams.topMargin;
                 // Вытягивание первого элемента из стека, остальные стоят на месте
                 if (edgeLimit >= currentTopEdge){
                    delta = 0;
                 } else {
                     delta = futureTopEdge - edgeLimit > 0 ? -dy : edgeLimit - currentTopEdge;
                 }
            // Элементы выше нижнего стека
            } else {
                // Если будущее положение элемента после скрола заезжает на верхний стек
                if (futureTopEdge < getBottomEdgeOfTopStack()){
                    // Кол-во элементов в стеке меньше максимального
                    if (getTopStackSize() <= mMaxElementsInStack){

                        int edgeLimit = layoutParams.topMargin;

                        if (viewCache.indexOfValue(view) != 0){
                            edgeLimit = getDecoratedTop(viewCache.get(currentPosition - 1)) + convertDpToPx(view.getResources(), ITEM_HEIGHT_IN_STACK_IN_DP);
                        }

                        // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                        delta = futureTopEdge - edgeLimit > 0 ? -dy : edgeLimit - currentTopEdge;
                        Log.d("StackLayoutManager", String.format("currentTopEdge %d futureTopEdge %d edgeLimit %d delta %d",
                                currentTopEdge, futureTopEdge, edgeLimit, delta));

                    } else {
                        // Кол-во элементов в стеке больше максимального
                        // Надо подвинуть второй видимы элемент на первый и когда они сравняются удалить первый
                        int edgeLimit = layoutParams.topMargin;


                        if (viewCache.indexOfValue(view) > 1) {
                            edgeLimit = getDecoratedTop(viewCache.get(currentPosition - 1)) + convertDpToPx(view.getResources(), ITEM_HEIGHT_IN_STACK_IN_DP);
                        }

                        // Если будущее положения меньше допустимого, выставляем смещение такое чтоб элемент доехал до допустимой границы
                        delta = futureTopEdge - edgeLimit > 0 ? -dy : edgeLimit - currentTopEdge;


                        if (viewCache.get(currentPosition - 1) != null &&
                                getDecoratedTop(viewCache.get(currentPosition - 1)) == currentTopEdge) {
                            removeAndRecycleView(viewCache.get(currentPosition - 1), recycler);
                            viewCache.remove(currentPosition - 1);
                        }
                    }
                }

            }

            attachView(view);
            view.offsetTopAndBottom(delta);
            currentPosition++;
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
