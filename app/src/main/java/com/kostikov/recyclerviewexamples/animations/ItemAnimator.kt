package com.kostikov.recyclerviewexamples.animations

import android.support.animation.DynamicAnimation
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.support.v7.widget.RecyclerView
import java.util.*


/**
 *
 * Базовый класс для создания анимации элементов
 *
 * @author Kostikov Aleksey.
 */
abstract class ItemAnimator(private val mViewHolder: RecyclerView.ViewHolder,
                            private val mAnimationEndCallback: (RecyclerView.ViewHolder) -> Unit) {
    private val mAnimations = ArrayList<SpringAnimation>()
    private val mSpringForce = SpringForce()

    /**
     * Пуст ли список анимаций
     *
     * @return true - список анимаций пуст, никакой анимации нет
     */
    val isEmpty: Boolean
        get() = mAnimations.isEmpty()


    init {

        mSpringForce.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY).stiffness = SpringForce.STIFFNESS_VERY_LOW

        animationInit(mViewHolder)
    }

    /**
     * Завершить анимацию
     */
    fun skipToEnd() {
        for (animation in mAnimations) {
            animation.skipToEnd()
        }
    }

    /**
     * Старт анимации
     */
    fun start() {
        for (animation in mAnimations) {
            animation.start()
        }
    }

    /**
     * Инициализация вида анимация в дочерних класса.
     * В нем используя методы: translateX, translateY, scaleX, scaleY, alpha задаем вид анимации
     *
     * @param viewHolder RecyclerView.ViewHolder
     */
    protected abstract fun animationInit(viewHolder: RecyclerView.ViewHolder)

    /**
     * Перемещение по горизонтали
     *
     * @param startValue начально значение
     * @param endValue конечное значение
     */
    protected fun translateX(startValue: Float, endValue: Float) {
        if (java.lang.Float.compare(startValue, endValue) != 0) {

            mViewHolder.itemView.translationX = startValue

            val springAnimation = SpringAnimation(mViewHolder.itemView, DynamicAnimation.TRANSLATION_X, endValue)
            mSpringForce.finalPosition = endValue
            springAnimation.spring = mSpringForce

            add(springAnimation)
        }
    }

    /**
     * Перемещение по вертикали
     *
     * @param startValue начально значение
     * @param endValue конечное значение
     */
    protected fun translateY(startValue: Float, endValue: Float) {
        if (java.lang.Float.compare(startValue, endValue) != 0) {
            mViewHolder.itemView.translationY = startValue

            val springAnimation = SpringAnimation(mViewHolder.itemView, DynamicAnimation.TRANSLATION_Y, endValue)
            mSpringForce.finalPosition = endValue
            springAnimation.spring = mSpringForce

            add(springAnimation)
        }
    }

    /**
     * Размер по горизонтали
     *
     * @param startValue начально значение
     * @param endValue конечное значение
     */
    protected fun scaleX(startValue: Float, endValue: Float) {
        if (java.lang.Float.compare(startValue, endValue) != 0) {
            mViewHolder.itemView.scaleX = startValue

            val springAnimation = SpringAnimation(mViewHolder.itemView, DynamicAnimation.SCALE_X, endValue)
            mSpringForce.finalPosition = endValue
            springAnimation.spring = mSpringForce

            add(springAnimation)
        }
    }

    /**
     * Размер по вертикали
     *
     * @param startValue начально значение
     * @param endValue конечное значение
     */
    protected fun scaleY(startValue: Float, endValue: Float) {
        if (java.lang.Float.compare(startValue, endValue) != 0) {
            mViewHolder.itemView.scaleY = startValue

            val springAnimation = SpringAnimation(mViewHolder.itemView, DynamicAnimation.SCALE_Y, endValue)
            mSpringForce.finalPosition = endValue
            springAnimation.spring = mSpringForce

            add(springAnimation)
        }
    }

    /**
     * Изменение альфы
     *
     * @param startValue начально значение
     * @param endValue конечное значение
     */
    protected fun alpha(startValue: Float, endValue: Float) {
        if (java.lang.Float.compare(startValue, endValue) != 0) {
            mViewHolder.itemView.alpha = startValue

            val springAnimation = SpringAnimation(mViewHolder.itemView, DynamicAnimation.ALPHA, endValue)
            mSpringForce.finalPosition = endValue
            springAnimation.spring = mSpringForce

            add(springAnimation)
        }
    }

    private fun add(anim: SpringAnimation) {
        anim.addEndListener { animation, canceled, value, velocity ->
            mAnimations.remove(anim)
            checkFinished()
        }

        mAnimations.add(anim)
    }

    private fun checkFinished() {
        if (mAnimations.isEmpty()) {
            mAnimationEndCallback(mViewHolder)
        }
    }

}
