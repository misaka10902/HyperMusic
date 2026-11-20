package top.yukonga.miuix.kmp.basic

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Stable
class NativeTextFieldHandle {
    internal var editText: EditText? = null
    fun requestFocus() { editText?.requestFocus() }
    fun clearFocus() { editText?.clearFocus() }


    fun requestFocusAndShowIme() {
        val view = editText ?: return
        view.requestFocus()
        view.post {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}

@Composable
fun rememberNativeTextFieldHandle(): NativeTextFieldHandle = remember { NativeTextFieldHandle() }

@Composable
fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    textStyle: TextStyle = TextStyle.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    handle: NativeTextFieldHandle? = null,
    onDone: (() -> Unit)? = null
) {
    val currentOnDone by rememberUpdatedState(onDone)
    val textColor = textStyle.color.takeIf { it != Color.Unspecified }
        ?: MiuixTheme.colorScheme.onSurface
    val hintColor = MiuixTheme.colorScheme.onSurfaceVariantActions
    val themeColor = MiuixTheme.colorScheme.primary

    val textWatcher = remember(onValueChange) {
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { onValueChange(it) }
            }
        }
    }

    AndroidView(
        factory = { context ->
            EditText(context).apply {
                handle?.editText = this
                setText(value)
                this.hint = hint
                this.isSingleLine = singleLine
                this.maxLines = maxLines
                setTextColor(textColor.toArgb())
                setHintTextColor(hintColor.toArgb())
                textSize = textStyle.fontSize.value

                val weight = textStyle.fontWeight?.weight ?: FontWeight.Medium.weight
                typeface = Typeface.create(typeface, weight, false)

                inputType = InputType.TYPE_CLASS_TEXT

                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START

                textAlignment = when (textAlign) {
                    TextAlign.Center -> EditText.TEXT_ALIGNMENT_CENTER
                    TextAlign.End, TextAlign.Right -> EditText.TEXT_ALIGNMENT_VIEW_END
                    else -> EditText.TEXT_ALIGNMENT_VIEW_START
                }

                if (maxLength < Int.MAX_VALUE) {
                    filters = arrayOf(InputFilter.LengthFilter(maxLength))
                }

                imeOptions = EditorInfo.IME_ACTION_DONE

                background = null
                setPadding(0, 0, 0, 0)

                textCursorDrawable?.setTint(themeColor.toArgb())

                highlightColor = themeColor.copy(alpha = 0.2f).toArgb()

                textSelectHandle?.setTint(themeColor.toArgb())
                textSelectHandleLeft?.setTint(themeColor.toArgb())
                textSelectHandleRight?.setTint(themeColor.toArgb())

                addTextChangedListener(textWatcher)

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        currentOnDone?.invoke()
                        currentOnDone != null
                    } else {
                        false
                    }
                }

                if (autoFocus) post { requestFocus() }
            }
        },
        update = { editText ->
            if (editText.text.toString() != value) {
                editText.removeTextChangedListener(textWatcher)
                editText.setText(value)
                editText.setSelection(editText.text.length)
                editText.addTextChangedListener(textWatcher)
            }
            editText.isEnabled = enabled

            editText.highlightColor = themeColor.copy(alpha = 0.2f).toArgb()

            if (autoFocus && !editText.isFocused) editText.requestFocus()
        },
        modifier = modifier
    )
}
