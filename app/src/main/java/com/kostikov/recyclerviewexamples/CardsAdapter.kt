package com.kostikov.recyclerviewexamples

import android.content.res.ColorStateList
import android.support.v7.util.DiffUtil
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find


/**
 * @author Kostikov Aleksey.
 */
class CardsAdapter(cardList: List<CardEntity>): RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    private var cardListBuf: MutableList<CardEntity>

    init {
        setHasStableIds(true)
        cardListBuf = cardList.toMutableList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val card = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
        return ViewHolder(card)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("CardsAdapter",  "bind $position position" )

        holder.bind(cardListBuf[position])
    }

    override fun getItemId(position: Int) = cardListBuf[position].id

    override fun getItemCount() = cardListBuf.size

    fun swapData( newCardList: List<CardEntity>) {
        val callback = CardsDiffUtilCallbacks(cardListBuf, newCardList )
        val result = DiffUtil.calculateDiff(callback)

        cardListBuf.clear()
        cardListBuf.addAll(newCardList)

        result.dispatchUpdatesTo(this)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val cardView by lazy {itemView.find<CardView>(R.id.card_view)}
        val textView by lazy {itemView.find<TextView>(R.id.card_text_view)}

        fun bind(cardData: CardEntity){
            cardView.setCardBackgroundColor(ColorStateList.valueOf(cardData.color))
            textView.text = cardData.description
        }
    }
}