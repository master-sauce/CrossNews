package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.network.AiService
import com.newsbias.tracker.network.ReaderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleAiState(
    val loading: Boolean = false,
    val summary: String? = null,
    val keyPoints: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ArticleAiViewModel @Inject constructor(
    private val reader: ReaderService,
    private val ai: AiService,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticleAiState())
    val state: StateFlow<ArticleAiState> = _state

    private var lastUrl: String? = null

    fun analyze(url: String) {
        if (_state.value.loading) return
        if (url == lastUrl && _state.value.summary != null) return  // cached
        lastUrl = url
        _state.value = ArticleAiState(loading = true)

        viewModelScope.launch {
            try {
                val text = reader.fetchClean(url)
                if (text.isNullOrBlank()) {
                    _state.value = ArticleAiState(error = "לא ניתן לטעון את תוכן הכתבה")
                    return@launch
                }

                val sys = "אתה עורך חדשות. ענה בעברית בלבד, קצר וממוקד."

                val summary = ai.prompt(
                    sys,
                    "סכם את הכתבה הבאה ב-3-4 משפטים ברורים:\n\n$text"
                )
                val keyPoints = ai.prompt(
                    sys,
                    "הוצא את 3-5 העובדות המרכזיות מהכתבה. כל עובדה בשורה נפרדת, מתחילה ב-•\n\n$text"
                )

                _state.value = ArticleAiState(
                    summary = summary ?: "(שגיאה בסיכום)",
                    keyPoints = keyPoints ?: "(שגיאה בעובדות)",
                )
            } catch (e: Exception) {
                _state.value = ArticleAiState(error = e.message ?: "שגיאה")
            }
        }
    }

    fun retry(url: String) {
        lastUrl = null
        analyze(url)
    }
}