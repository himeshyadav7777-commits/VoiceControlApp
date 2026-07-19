package com.voicecontrol.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusText: TextView

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val speakButton = findViewById<Button>(R.id.speakButton)

        requestAllPermissions()

        speakButton.setOnClickListener {
            startListening()
        }
    }

    private fun requestAllPermissions() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull() ?: ""
                statusText.text = "सुना: $command"
                handleCommand(command)
            }

            override fun onError(error: Int) {
                statusText.text = "फिर से बोलें, समझ नहीं आया"
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun handleCommand(command: String) {
        val lower = command.trim()

        when {
            lower.contains("कॉल") -> {
                val name = extractName(lower, listOf("कॉल करो", "को कॉल करो", "कॉल"))
                if (name.isNotBlank()) makeCall(name) else toast("नाम समझ नहीं आया")
            }
            lower.contains("मैसेज") || lower.contains("संदेश") -> {
                val rest = extractName(lower, listOf("मैसेज भेजो", "संदेश भेजो"))
                val parts = rest.split(" ", limit = 2)
                if (parts.size == 2) {
                    sendSms(parts[0], parts[1])
                } else {
                    toast("मैसेज के लिए नाम और टेक्स्ट दोनों बोलें")
                }
            }
            else -> toast("कमांड समझ नहीं आया")
        }
    }

    private fun extractName(command: String, triggers: List<String>): String {
        var result = command
        for (t in triggers) {
            result = result.replace(t, "").trim()
        }
        return result
    }

    private fun makeCall(contactName: String) {
        val number = ContactHelper.findNumberByName(this, contactName)
        if (number == null) {
            toast("$contactName नाम का कॉन्टैक्ट नहीं मिला")
            return
        }
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
        }
        startActivity(intent)
    }

    private fun sendSms(contactName: String, text: String) {
        val number = ContactHelper.findNumberByName(this, contactName)
        if (number == null) {
            toast("$contactName नाम का कॉन्टैक्ट नहीं मिला")
            return
        }
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(number, null, text, null, null)
        toast("$contactName को मैसेज भेज दिया")
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        statusText.text = msg
    }
}