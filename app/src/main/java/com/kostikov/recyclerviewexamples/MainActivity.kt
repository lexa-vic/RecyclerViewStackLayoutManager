package com.kostikov.recyclerviewexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(recyclerView){

            layoutManager = StackLayoutManager()
            adapter = CardsAdapter(cardsListData)
            itemAnimator = SampleItemAnimator()

            setHasFixedSize(true)
        }

        /*fabAll.setOnClickListener {
            (recyclerView.adapter as CardsAdapter).swapData(cardsListData) }
        fabDell.setOnClickListener {
            (recyclerView.adapter as CardsAdapter).swapData(cardsListData.filter {
                cardEntity -> cardEntity.id.toInt() % 2 == 0 })
        }*/
    }

}
