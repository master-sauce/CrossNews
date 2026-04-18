package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.network.AiService
import com.newsbias.tracker.network.ReaderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareAiState(
    val loading: Boolean = false,
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
        _state.value = CompareAiState(loading = true)

        viewModelScope.launch {
            try {
                val (leftText, rightText) = coroutineScope {
                    val a = async { reader.fetchClean(leftUrl) }
                    val b = async { reader.fetchClean(rightUrl) }
                    a.await() to b.await()
                }

                if (leftText.isNullOrBlank() || rightText.isNullOrBlank()) {
                    _state.value = CompareAiState(error = "לא ניתן לטעון את תוכן הכתבות")
                    return@launch
                }

                val sys = "אתה עורך חדשות. ענה בעברית בלבד, קצר וממוקד."

                coroutineScope {
                    val a = async {
                        ai.prompt(sys, "סכם את הכתבה הבאה ב-3 משפטים:\n\n$leftText")
                    }
                    val b = async {
                        ai.prompt(sys, "סכם את הכתבה הבאה ב-3 משפטים:\n\n$rightText")
                    }
                    val c = async {
                        ai.prompt(
                            sys,
                            "השווה בין שתי הכתבות הבאות. מה ההבדלים בדגשים, בסגנון ובהטיה אפשרית? ענה ב-4-5 משפטים.\n\n=== כתבה 1 ===\n$leftText\n\n=== כתבה 2 ===\n$rightText"
                        )
                    }
                    _state.value = CompareAiState(
                        leftSummary = a.await() ?: "(שגיאה)",
                        rightSummary = b.await() ?: "(שגיאה)",
                        comparison = c.await() ?: "(שגיאה)",
                    )
                }
            } catch (e: Exception) {
                _state.value = CompareAiState(error = e.message ?: "שגיאה")
            }
        }
    }

    fun dismiss() { _state.value = CompareAiState() }
}