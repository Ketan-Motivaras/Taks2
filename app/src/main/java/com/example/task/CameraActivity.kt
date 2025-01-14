package com.example.task

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private var cameraId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.btnCapture)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                texture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(texture: android.graphics.SurfaceTexture): Boolean =
                true

            override fun onSurfaceTextureUpdated(texture: android.graphics.SurfaceTexture) {}
        }

        captureButton.setOnClickListener {
            captureImage()
        }
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK
        } ?: return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }

    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val previewSurface = Surface(surfaceTexture)

        // Initialize ImageReader with proper dimensions
        imageReader = ImageReader.newInstance(
            textureView.width,
            textureView.height,
            android.graphics.ImageFormat.JPEG,
            1
        )
        val imageReaderSurface = imageReader.surface

        // Build capture request for preview
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        // Add both preview and ImageReader surfaces to the session
        cameraDevice.createCaptureSession(
            listOf(previewSurface, imageReaderSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    cameraCaptureSession.setRepeatingRequest(
                        captureRequestBuilder.build(),
                        null,
                        null
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Failed to configure camera preview",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            null
        )
    }


    private fun captureImage() {
        val captureRequestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)

        // Set a listener for when the image is available
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            // Close the image to avoid memory leaks
            image.close()

            // Scale the bitmap to match the TextureView size
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                textureView.width,
                textureView.height,
                true
            )

            // Calculate viewport dimensions on the screen
            val viewport = findViewById<View>(R.id.view_port)
            val viewportLeft = viewport.left
            val viewportTop = viewport.top
            val viewportWidth = viewport.width
            val viewportHeight = viewport.height

            // Scale viewport dimensions to match the scaled bitmap
            val scaleX = scaledBitmap.width.toFloat() / textureView.width
            val scaleY = scaledBitmap.height.toFloat() / textureView.height

            val cropLeft = (viewportLeft * scaleX).toInt()
            val cropTop = (viewportTop * scaleY).toInt()
            val cropWidth = (viewportWidth * scaleX).toInt()
            val cropHeight = (viewportHeight * scaleY).toInt()

            // Ensure crop dimensions are within the bounds of the scaled bitmap
            val croppedBitmap = Bitmap.createBitmap(
                scaledBitmap,
                cropLeft.coerceIn(0, scaledBitmap.width - 1),
                cropTop.coerceIn(0, scaledBitmap.height - 1),
                cropWidth.coerceAtMost(scaledBitmap.width - cropLeft),
                cropHeight.coerceAtMost(scaledBitmap.height - cropTop)
            )

            // Save the cropped image to a file
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "captured_image.jpg")
            FileOutputStream(file).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Open the preview activity
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("imagePath", file.absolutePath)
            startActivity(intent)
        }, null)

        // Capture the image
        cameraCaptureSession.capture(captureRequestBuilder.build(), null, null)
    }


}
