package com.benediction.noteshub

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.benediction.noteshub.ui.theme.NotesHubTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import java.io.OutputStream

class MainActivity : ComponentActivity() {

    private var contentResolver: ContentResolver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesHubTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    scanner.getStartScanIntent(this)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to start scanning", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        contentResolver = applicationContext.contentResolver
    }

    private val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(2)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

    private val scanner = GmsDocumentScanning.getClient(options)

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val result =
                    GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                result?.getPages()?.let { pages ->
                    for (page in pages) {
                        val imageUri = page.imageUri
                        saveImageToStorage(imageUri)
                    }
                }
                result?.getPdf()?.let { pdf ->
                    val pdfUri = pdf.getUri()
                    val pageCount = pdf.getPageCount()
                }
            }
        }

    private fun saveImageToStorage(imageUri: Uri) {
        val inputStream = contentResolver?.openInputStream(imageUri)
        val outputStream: OutputStream?

        try {
            inputStream?.let {
                val fileName = "scanned_image_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }

                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val imageUri = contentResolver?.insert(contentUri, values)
                outputStream = imageUri?.let { contentResolver?.openOutputStream(it) }

                if (outputStream != null) {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input?.copyTo(output)
                        }
                    }

                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to create output stream", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Failed to open input stream", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            inputStream?.close()
        }
    }

}
