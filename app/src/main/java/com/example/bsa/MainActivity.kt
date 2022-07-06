package com.example.bsa

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bsa.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bodyImageView.setOnSelectionListener {calculatedSurfaceArea: Float ->
            updateCalculateSurfaceArea(calculatedSurfaceArea)
        }

        binding.bodyImageView.loadState(savedInstanceState)
        updateCalculateSurfaceArea(binding.bodyImageView.calculatedSurfaceArea)

    }

    private fun updateCalculateSurfaceArea(calculatedSurfaceArea: Float) {
        /*
         * Print the new value with animation
         */
        Thread { // wait in another thread not to block ui thread
            val text = binding.surfaceAreaText.text
            var oldValue:Float = text.substring(0 until text.length-2).toFloat()
            if (calculatedSurfaceArea != oldValue) {
                val factor = if (calculatedSurfaceArea > oldValue) 1 else -1
                val interval = 100 / abs(calculatedSurfaceArea - oldValue) // in milliseconds
                while (abs(calculatedSurfaceArea - oldValue) >= 1) {
                    oldValue += factor
                    runOnUiThread { // but update in ui thread
                        binding.surfaceAreaText.text =
                            resources.getString(R.string.surface_area, oldValue)
                        binding.surfaceAreaText.invalidate()
                    }
                    Thread.sleep(interval.toLong())
                }
                binding.surfaceAreaText.text =
                    resources.getString(R.string.surface_area,calculatedSurfaceArea)
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        binding.bodyImageView.saveState(outState)
    }

}
