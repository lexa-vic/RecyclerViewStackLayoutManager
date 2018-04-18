package com.kostikov.recyclerviewexamples.animations

import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import java.util.*

/**
 * Аниматор появления сегментов со Spring анимацией
 *
 * @author Kostikov Aleksey.
 */
class SpringAppearanceAnimator : RecyclerView.ItemAnimator() {

    private val mAnimationsMap = HashMap<RecyclerView.ViewHolder, ItemAnimator>()

    private var mPrevElementTop = 0


    override fun runPendingAnimations() {
        val handler = Handler()

        val sortedMap = SparseArray<MutableMap.MutableEntry<RecyclerView.ViewHolder, ItemAnimator>>()

        for (animEntry in mAnimationsMap.entries) {
            sortedMap.put(animEntry.key.layoutPosition, animEntry)
        }

        for (i in 0 until sortedMap.size()) {
            val currentAnimation = sortedMap.get(i).value
            var delayElementFactor = 0
            val currentTop = sortedMap.get(i).key.itemView.getTop()
            val elementHeight = sortedMap.get(i).key.itemView.getHeight()

            if (mPrevElementTop != 0 && currentTop - mPrevElementTop < elementHeight / 4) {
                delayElementFactor = BOTTOM_ELEMENT_DELAY_FACTOR
            }

            handler.postDelayed({ currentAnimation.start() },
                    (sortedMap.get(i).key.getLayoutPosition() * ELEMENTS_DELAY + ELEMENTS_DELAY * delayElementFactor).toLong())

            mPrevElementTop = sortedMap.get(i).key.itemView.getTop()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun endAnimation(item: RecyclerView.ViewHolder) {
        mAnimationsMap[item]?.skipToEnd()
    }

    /**
     * {@inheritDoc}
     */
    override fun endAnimations() {
        for ((_, value) in mAnimationsMap) {
            value.skipToEnd()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun isRunning(): Boolean {
        return !mAnimationsMap.isEmpty()
    }

    /**
     * {@inheritDoc}
     */
    override fun animateDisappearance(viewHolder: RecyclerView.ViewHolder,
                                      preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
                                      postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo?): Boolean {
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun animateAppearance(viewHolder: RecyclerView.ViewHolder,
                                   preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo?,
                                   postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        return processAnimation(viewHolder, AddAnimation(viewHolder, this::finishAnimation))
    }

    /**
     * {@inheritDoc}
     */
    override fun animatePersistence(viewHolder: RecyclerView.ViewHolder,
                                    preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
                                    postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun animateChange(oldHolder: RecyclerView.ViewHolder,
                               newHolder: RecyclerView.ViewHolder,
                               preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
                               postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        return false
    }

    private fun processAnimation(viewHolder: RecyclerView.ViewHolder, animation: ItemAnimator): Boolean {
        val result: Boolean

        if (animation.isEmpty) {
            dispatchAnimationFinished(viewHolder)
            result = false
        } else {
            viewHolder.itemView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            viewHolder.itemView.buildLayer()

            mAnimationsMap[viewHolder] = animation
            result = true
        }

        return result
    }

    private fun finishAnimation(viewHolder: RecyclerView.ViewHolder) {
        mAnimationsMap.remove(viewHolder)
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.scaleX = 1f
        viewHolder.itemView.scaleY = 1f
        viewHolder.itemView.alpha = 1f

        viewHolder.itemView.setLayerType(View.LAYER_TYPE_NONE, null)

        mPrevElementTop = 0

        if (viewHolder.isRecyclable) {
            dispatchAnimationFinished(viewHolder)
        }
    }

    class AddAnimation (viewHolder: RecyclerView.ViewHolder,
                       animationEndCallback:  (RecyclerView.ViewHolder) -> Unit) : ItemAnimator(viewHolder, animationEndCallback) {

        /**
         * {@inheritDoc}
         */
        override fun animationInit(viewHolder: RecyclerView.ViewHolder) {
            val height = viewHolder.itemView.resources.displayMetrics.heightPixels
            translateY(height.toFloat(), 0f)
        }
    }

    companion object {

        private val ELEMENTS_DELAY = 100

        private val BOTTOM_ELEMENT_DELAY_FACTOR = 2
    }
}
