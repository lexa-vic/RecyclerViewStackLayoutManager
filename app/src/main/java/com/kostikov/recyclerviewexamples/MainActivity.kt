package com.kostikov.recyclerviewexamples

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.kostikov.recyclerviewexamples.animations.SpringAppearanceAnimator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(recyclerView){
            layoutManager = StackLayoutManager()
            adapter = CardsAdapter(emptyList())
            itemAnimator = SpringAppearanceAnimator()

            setHasFixedSize(true)
        }

        val handler = Handler()

        handler.postDelayed({
           (recyclerView.adapter as CardsAdapter).swapData(cardsListData, true)
            recyclerView.adapter.notifyItemRangeInserted(0, cardsListData.size)
        }, 200)
    }

}
