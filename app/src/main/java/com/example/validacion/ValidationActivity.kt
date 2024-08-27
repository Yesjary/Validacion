package com.example.validacion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException


class ValidationActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var logOutButton: Button
    private lateinit var addPersonButton: Button
    private lateinit var validatePersonButton: Button
    private lateinit var nameInput: EditText
    private var isAddingPerson = false

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { handleCapturedImage(it) }
    }

    private var currentBitmap: Bitmap? = null

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validation)


         // Verificar y solicitar permisos de notificaciones
        //checkNotificationPermission()

       // NotiUtils.createNotificationChannel(this)

        // Comprobar permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        logOutButton = findViewById(R.id.logOutButton)
        validatePersonButton = findViewById(R.id.buttonTakePicture)
        nameInput = findViewById(R.id.nameEditText)

        logOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        validatePersonButton.setOnClickListener {
            // Determina la acción que se debe realizar
            isAddingPerson = nameInput.text.isNotEmpty() // Si el nombre está vacío, no está en modo agregar persona
            takePicture.launch(null)
        }
    }

    private fun handleCapturedImage(imageBitmap: Bitmap) {
        Log.d("ValidationActivity", "Captured image: $imageBitmap")
        currentBitmap = imageBitmap

        if (isAddingPerson) {
            val name = nameInput.text.toString()
            addPerson(name, imageBitmap)
        } else {
            validatePerson(imageBitmap)
        }
    }

    private fun addPerson(name: String, imageBitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val file = File(filesDir, "image.jpg")
        file.writeBytes(byteArray)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", name)
            .addFormDataPart("image", file.name, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url("  http://192.168.2.67:5000/add_person") // Asegúrate de que esta URL sea correcta
            .post(requestBody)
            .build()

        executeRequest(request)
    }

    private fun validatePerson(imageBitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val file = File(filesDir, "image.jpg")
        file.writeBytes(byteArray)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(" http://192.168.2.67:5000/validate_person") // Asegúrate de que esta URL sea correcta
            .post(requestBody)
            .build()

        executeRequest(request)
    }

    private fun executeRequest(request: Request) {
        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("ValidationActivity", "Response body: $responseBody")
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        showMessage("Operación exitosa: $responseBody")
                    } else {
                        showMessage("Error en la operación.")
                    }
                }
            } catch (e: IOException) {
                Log.e("ValidationActivity", "Error de red: ", e)
                runOnUiThread {
                    showMessage("Error de red: ${e.message}")
                }
            }
        }.start()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

