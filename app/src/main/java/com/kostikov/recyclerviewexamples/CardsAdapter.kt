package com.kostikov.recyclerviewexamples

import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
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

    private val cardColors = listOf(
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val card = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)

        return ViewHolder(card)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.cardView.cardBackgroundColor = ColorStateList.valueOf(cardColors[position % cardColors.size])
        holder.textView.text = position.toString()
    }

    override fun getItemCount() = cardColors.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val cardView by lazy {itemView.find<CardView>(R.id.card_view)}
        val textView by lazy {itemView.find<TextView>(R.id.card_text_view)}
    }

}