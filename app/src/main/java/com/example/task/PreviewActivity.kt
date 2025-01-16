package com.example.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.task.databinding.ActivityPreviewBinding


class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra("imagePath")
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val correctedBitmap = adjustOrientation(bitmap)
            binding.previewImage.setImageBitmap(correctedBitmap)
        } else {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    // Simplified adjust orientation method
    private fun adjustOrientation(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f) // Rotate the image to upright orientation
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

