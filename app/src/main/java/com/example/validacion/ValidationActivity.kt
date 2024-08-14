package com.example.validacion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ValidationActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var logOutButton: Button

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { handleCapturedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validation)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        logOutButton = findViewById(R.id.logOutButton)
        logOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            // Redirigir a la actividad de autenticación
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish() // Opcional: cerrar la actividad actual
        }

        findViewById<Button>(R.id.button_take_picture).setOnClickListener {
            try {
                takePicture.launch(null)
            } catch (e: Exception) {
                Log.e("ValidationActivity", "Error launching camera: ", e)
            }
        }
    }

    private fun handleCapturedImage(imageBitmap: Bitmap) {
        Log.d("ValidationActivity", "Captured image: $imageBitmap")

        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val file = File(filesDir, "image.jpg")
        file.writeBytes(byteArray)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", "Allan")
            .addFormDataPart("image", file.name, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(" http://192.168.1.37:5000/add_and_validate_person")
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("ValidationActivity", "Response body: $responseBody")
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val found = jsonResponse.optBoolean("found", false)
                            val message = jsonResponse.optString("message", "Error en la respuesta del servidor")

                            showMessage(message)

                            if (found) {
                                Log.d("ValidationActivity", "Persona encontrada en la base de datos.")
                            } else {
                                Log.d("ValidationActivity", "Persona no encontrada en la base de datos.")
                            }
                        } catch (e: JSONException) {
                            Log.e("ValidationActivity", "Error al parsear JSON: ", e)
                            showMessage("Error al procesar la respuesta del servidor.")
                        }
                    } else {
                        Log.e("ValidationActivity", "Respuesta no exitosa o cuerpo vacío.")
                        showMessage("Error en la comunicación con el servidor.")
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
