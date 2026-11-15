package com.example.scenicguide.voice

/**
 * 把长文本切成不超过 500 字的片段，优先在句末标点处断开，避免把句子拦腰截断。
 */
object TtsTextSplitter {
    const val MAX_LENGTH = 500

    fun split(text: String, maxLength: Int = MAX_LENGTH): List<String> {
        require(maxLength > 0) { "maxLength must be positive" }
        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                result += paragraph.toString()
                paragraph.setLength(0)
            }
        }

        for (line in normalized.split('\n')) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            for (sentence in splitSentences(trimmedLine)) {
                if (sentence.length > maxLength) {
                    flushParagraph()
                    result += sentence.chunked(maxLength)
                    continue
                }
                if (paragraph.length + sentence.length > maxLength) {
                    flushParagraph()
                }
                paragraph.append(sentence)
            }
        }
        flushParagraph()
        return result
    }

    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val buffer = StringBuilder()
        for (char in text) {
            buffer.append(char)
            if (char in SENTENCE_ENDS) {
                sentences += buffer.toString()
                buffer.setLength(0)
            }
        }
        if (buffer.isNotEmpty()) sentences += buffer.toString()
        return sentences
    }

    private const val SENTENCE_ENDS = "。！？；.!?;"
}
