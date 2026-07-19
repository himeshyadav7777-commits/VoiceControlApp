package com.voicecontrol.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat

class CallListenerService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()
        listenForAnswerCommand()
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "call_listener_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Call Listener", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Voice Control")
            .setContentText("आ रही कॉल के लिए सुन रहा हूँ... 'उठा लो' बोलें")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()
        startForeground(101, notification)
    }

    private fun listenForAnswerCommand() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()?.trim() ?: ""
                if (heard.contains("उठा") || heard.contains("उठाओ") || heard.contains("उठा लो")) {
                    answerCall()
                } else {
                    listenForAnswerCommand()
                }
            }

            override fun onError(error: Int) {
                listenForAnswerCommand()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun answerCall() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        try {
            telecomManager.acceptRingingCall()
        } catch (e: SecurityException) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}