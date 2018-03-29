package com.kostikov.recyclerviewexamples

import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v7.util.DiffUtil
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find


/**
 * @author Kostikov Aleksey.
 */
class CardsAdapter: RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    private val firstCardColors = listOf(
            Color.parseColor("#E57373"),
            Color.parseColor("#F06292"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#9575CD"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#4FC3F7"),
            Color.parseColor("#4DD0E1"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#81C784"),
            Color.parseColor("#AED581"),
            Color.parseColor("#DCE775"),
            Color.parseColor("#FFF176"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#A1887F"),
            Color.parseColor("#E0E0E0"),
            Color.parseColor("#90A4AE")
    )

    private val secondCardColors = listOf(
            Color.parseColor("#E57373"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#4FC3F7"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#DCE775"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#E0E0E0")
    )

    private var cardsList = firstCardColors

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val card = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
        return ViewHolder(card)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("CardsAdapter",  "$position position" )
        holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(cardsList[position % cardsList.size]))
        holder.textView.text = position.toString()
    }

    override fun getItemCount() = cardsList.size

    fun changeList(){
        if (cardsList ==(firstCardColors)){
            val diffUtils = DiffUtilsCallbacks(firstCardColors, secondCardColors)
            val result =  DiffUtil.calculateDiff(diffUtils)

            cardsList = secondCardColors
            result.dispatchUpdatesTo(this)
        } else {
            val diffUtils = DiffUtilsCallbacks(secondCardColors, firstCardColors)
            val result =  DiffUtil.calculateDiff(diffUtils)

            cardsList = firstCardColors
            result.dispatchUpdatesTo(this)
        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val cardView by lazy {itemView.find<CardView>(R.id.card_view)}
        val textView by lazy {itemView.find<TextView>(R.id.card_text_view)}
    }

    class DiffUtilsCallbacks(val oldList: List<Int>, val newList: List<Int>): DiffUtil.Callback(){

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getOldListSize() = oldList.size


        override fun getNewListSize() = newList.size


        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }
    }

}