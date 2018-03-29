package com.kostikov.recyclerviewexamples

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        with(recyclerView){

            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = CardsAdapter()
            itemAnimator = CustomItemAnimator()

            setHasFixedSize(true)
        }

        fabAdd.setOnClickListener { (recyclerView.adapter as CardsAdapter).changeList() }
    }
}
