package com.example.spamblackhole2

import android.content.Context
import android.util.Log

import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.jvm.Throws

class SpamClassifier(private val context: Context) {
    private val interpreter: Interpreter
    private val wordIndex: Map<String, Int>
    private val maxLen = 50
    private val vocabSize = 500
    private val oovToken = "<OOV>"

    init{
        interpreter = Interpreter(loadModelFile())
        wordIndex = loadWordIndex()
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer{
        val assetFileDescriptor = context.assets.openFd("spam_ham_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(text: String): String {
        val input = preprocessText(text)

        val output = Array(1){FloatArray(1)} // [[value]]
        // Debugging output
        Log.d("SpamClassifier", "Model input: ${input.contentToString()}")

        interpreter.run(arrayOf(input), output)
        Log.d("SpamClassifier", "Model output: ${output.contentDeepToString()}")

        val prediction = output[0][0]
        return if (prediction > 0.5) "Ham" else "Spam"
    }

    private fun loadWordIndex(): Map<String, Int>{
        val jsonString = context.assets.open("word_index.json").bufferedReader().use{it.readText()}
        val jsonObject = JSONObject(jsonString)
        val wordIndex = mutableMapOf<String, Int>()

        jsonObject.keys().forEach{
            wordIndex[it] = jsonObject.getInt(it)
        }
        val limitedWordIndex = wordIndex.filterValues {
            Log.d("vocabSize", it.toString())
            it < vocabSize }.toMutableMap()

        limitedWordIndex[oovToken] = 1
        return limitedWordIndex
    }

    private fun preprocessText(text: String): FloatArray {
        // 소문자 변환 및 특수 문자 제거
        val normalizedText = text.lowercase(Locale.ROOT).replace("[^a-zA-Z\\s]".toRegex(), "")

        // 토큰화 및 인덱싱
        val textToToken = normalizedText.split("\\s+.toRegex()")[0]
        val tokens = textToToken.split(" ")

        val sequence = tokens.map {
            val index = wordIndex[it] ?: wordIndex[oovToken] ?: 1  // OOV 토큰 처리
            index
        }

        val paddedSequence = sequence.take(maxLen).toMutableList()

        while (paddedSequence.size < maxLen) {
            paddedSequence.add(0)
        }

        return paddedSequence.map { it.toFloat() }.toFloatArray()
    }
}