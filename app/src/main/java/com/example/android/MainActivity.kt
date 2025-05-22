package com.example.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.calculator_button).setOnClickListener {
            val intent = Intent(this, Calculator::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.mplayer_button).setOnClickListener {
            val intent = Intent(this, MPlayer::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.location_button).setOnClickListener {
            val intent = Intent(this, Location::class.java)
            startActivity(intent)
        }

    }
}