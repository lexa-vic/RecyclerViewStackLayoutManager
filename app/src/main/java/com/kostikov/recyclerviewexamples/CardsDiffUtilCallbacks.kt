package com.kostikov.recyclerviewexamples

import android.support.v7.util.DiffUtil

/**
 * @author Kostikov Aleksey.
 */
class CardsDiffUtilCallbacks(val oldList: List<CardEntity>, val newList: List<CardEntity>) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}