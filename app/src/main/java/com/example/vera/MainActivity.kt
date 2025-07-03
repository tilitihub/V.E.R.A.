// MainActivity.kt

import android.Manifest // Szükséges
import android.content.Intent // Szükséges
import android.content.pm.PackageManager // Szükséges
import android.os.Bundle // Általában szükséges egy Activity-ben
import android.speech.RecognizerIntent // Szükséges
import android.speech.SpeechRecognizer // Szükséges (ha a speechRecognizer tagváltozó)
import android.util.Log // Szükséges a logoláshoz
import android.widget.ImageButton // Feltételezve, hogy van micButton
import android.widget.TextView // Feltételezve, hogy van inputTextView
import android.widget.Toast // Szükséges
import androidx.appcompat.app.AppCompatActivity // Vagy amilyen Activity-t használsz
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat // Szükséges
import androidx.core.content.ContextCompat // Szükséges

// import androidx.compose.ui.semantics.text // EZT TÖRÖLD, HA NINCS RÁ SZÜKSÉG (valószínűleg nincs)

class MainActivity :
    AppCompatActivity() { // Győződj meg róla, hogy az Activity megfelelően van deklarálva

    // Tagváltozók (győződj meg róla, hogy ezek léteznek és inicializálva vannak)
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private lateinit var inputTextView: TextView
    private lateinit var micButton: ImageButton // Ha a gomb állapotát is változtatod
    private val RECORD_AUDIO_PERMISSION_CODE = 102 // Ennek is deklarálva kell lennie

    // ... (onCreate, setupSpeechRecognizer, TextToSpeech részek, OpenAIClient stb.)

    private fun handleMicButtonClick() {
        // 1. Engedély ellenőrzése
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Engedély megvan

            // 2. Ellenőrizzük, hogy már hallgatózik-e
            if (isListening) {
                // Ha már hallgatózik, állítsuk le
                Log.i("SpeechRecognizer", "Mikrofon gomb: Hallgatózás leállítása.")
                speechRecognizer.stopListening()
                // Az 'isListening' és a micButton állapotát a RecognitionListener fogja frissíteni
                // az onEndOfSpeech vagy onError eseményeknél. Ez a megközelítés jó.
            } else {
                // Ha nem hallgatózik, indítsuk el a figyelést
                Log.i("SpeechRecognizer", "Mikrofon gomb: Hallgatózás indítása.")

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hu-HU")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hu-HU")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Mondj valamit...")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Offline felismerés preferálása (ha ezt szeretnéd):
                    // putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                try {
                    speechRecognizer.startListening(intent)
                    inputTextView.text = "Hallgatlak..." // Vizuális visszajelzés
                    // Az 'isListening' és a micButton állapotát a RecognitionListener.onReadyForSpeech
                    // metódusa fogja beállítani.
                    Log.d("SpeechRecognizer", "startListening meghívva.")
                } catch (e: SecurityException) {
                    Log.e(
                        "SpeechRecognizer",
                        "SecurityException a startListening közben (engedélyhiba?): ",
                        e
                    )
                    Toast.makeText(
                        this,
                        "Hiba a hangfelismerő indításakor (biztonsági okokból).",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.e("SpeechRecognizer", "Általános hiba a startListening közben: ", e)
                    Toast.makeText(this, "Hiba a hangfelismerő indításakor.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            // 3. Ha nincs engedély, kérjük be
            Log.i("SpeechRecognizer", "RECORD_AUDIO engedély nincs megadva, kérés indítása.")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    // ... (onRequestPermissionsResult, onDestroy, stb.)
}