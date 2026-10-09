package com.example.csvtango.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** 지금 소리 내어 읽고 있는 대상. 버튼을 강조하는 데 쓴다. */
enum class SpeechTarget { WORD, SENTENCE }

/** 일본어 읽어 주기. (웹 버전의 Web Speech API 자리) */
class JapaneseSpeaker(context: Context) {

    /** 재생 중인 대상. 없으면 null. */
    var speaking by mutableStateOf<SpeechTarget?>(null)
        private set

    /** 이 기기에서 일본어 음성을 쓸 수 있는지. 초기화가 끝나야 확정된다. */
    var japaneseAvailable by mutableStateOf(true)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null

    init {
        val tts = TextToSpeech(context.applicationContext) { status ->
            // engine 이 대입된 뒤에 보도록 메인 큐 뒤로 미룬다.
            main.post { japaneseAvailable = status == TextToSpeech.SUCCESS && useJapanese() }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = clear(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = clear(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = clear(utteranceId)

            private fun clear(utteranceId: String?) {
                main.post { if (speaking?.name == utteranceId) speaking = null }
            }
        })
        engine = tts
    }

    fun speak(text: String, target: SpeechTarget, rate: Float) {
        if (text.isBlank()) return
        val tts = engine ?: return

        stop()
        useJapanese()
        tts.setSpeechRate(rate)
        speaking = target
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, target.name)
    }

    fun stop() {
        engine?.stop()
        speaking = null
    }

    /** 일본어로 맞추고, 이 기기가 일본어를 읽을 수 있으면 true. */
    private fun useJapanese(): Boolean {
        val result = engine?.setLanguage(Locale.JAPANESE) ?: return false
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun shutdown() {
        engine?.shutdown()
        engine = null
        speaking = null
    }
}

/** `し / よん`, `1, 일` 처럼 여러 읽기가 있으면 첫 번째만 읽는다. (단어 전용) */
fun firstReading(text: String): String =
    text.split('/', ',', '、', '･', '・', '|').first().trim()

@Composable
fun rememberJapaneseSpeaker(): JapaneseSpeaker {
    val context = LocalContext.current.applicationContext
    val speaker = remember { JapaneseSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
