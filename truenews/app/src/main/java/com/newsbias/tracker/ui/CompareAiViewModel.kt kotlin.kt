package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.network.AiService
import com.newsbias.tracker.network.ReaderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareAiState(
    val loading: Boolean = false,
    val stage: String = "",
    val leftSummary: String? = null,
    val rightSummary: String? = null,
    val comparison: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CompareAiViewModel @Inject constructor(
    private val reader: ReaderService,
    private val ai: AiService,
) : ViewModel() {

    private val _state = MutableStateFlow(CompareAiState())
    val state: StateFlow<CompareAiState> = _state

    fun analyze(leftUrl: String, rightUrl: String) {
        if (_state.value.loading) return
        _state.value = CompareAiState(loading = true, stage = "טוען תוכן כתבה 1...")

        viewModelScope.launch {
            val sys = "אתה עורך חדשות. ענה בעברית בלבד, קצר וממוקד."

            // 1. Fetch left
            val leftText = reader.fetchClean(leftUrl)
            if (leftText.isNullOrBlank()) {
                _state.value = CompareAiState(error = "לא ניתן לטעון את כתבה 1")
                return@launch
            }

            _state.update { it.copy(stage = "טוען תוכן כתבה 2...") }

            // 2. Fetch right
            val rightText = reader.fetchClean(rightUrl)
            if (rightText.isNullOrBlank()) {
                _state.value = CompareAiState(error = "לא ניתן לטעון את כתבה 2")
                return@launch
            }

            // 3. Summarize left
            _state.update { it.copy(stage = "מסכם כתבה 1...") }
            val leftSummary = ai.prompt(sys, "סכם את הכתבה הבאה ב-3 משפטים:\n\n$leftText")
            _state.update { it.copy(leftSummary = leftSummary ?: "(שגיאה בסיכום)") }

            // 4. Summarize right
            _state.update { it.copy(stage = "מסכם כתבה 2...") }
            val rightSummary = ai.prompt(sys, "סכם את הכתבה הבאה ב-3 משפטים:\n\n$rightText")
            _state.update { it.copy(rightSummary = rightSummary ?: "(שגיאה בסיכום)") }

            // 5. Compare (use summaries to save tokens + avoid long context fails)
            _state.update { it.copy(stage = "משווה בין הכתבות...") }
            val comparison = ai.prompt(
                sys,
                "השווה בין שני הדיווחים הבאים. מה ההבדלים בדגשים, סגנון והטיה אפשרית? ענה ב-4-5 משפטים.\n\n" +
                        "=== דיווח 1 ===\n${leftSummary ?: leftText.take(2000)}\n\n" +
                        "=== דיווח 2 ===\n${rightSummary ?: rightText.take(2000)}"
            )

            _state.update {
                it.copy(
                    loading = false,
                    stage = "",
                    comparison = comparison ?: "(שגיאה בהשוואה — נסה שוב)",
                )
            }
        }
    }

    fun retry(leftUrl: String, rightUrl: String) {
        _state.value = CompareAiState()
        analyze(leftUrl, rightUrl)
    }

    fun dismiss() { _state.value = CompareAiState() }
}