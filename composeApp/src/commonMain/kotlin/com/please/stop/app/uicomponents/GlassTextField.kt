package com.please.stop.app.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.please.stop.app.theme.LocalAppColors

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val appColors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        prefix = prefix,
        supportingText = supportingText,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = appColors.cardGlass.copy(alpha = 0.24f),
            unfocusedContainerColor = appColors.cardGlass.copy(alpha = 0.24f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = appColors.cardGlass.copy(alpha = 0.52f),
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = appColors.cardGlassBorder,
                shape = RoundedCornerShape(16.dp),
            ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldWithLabelPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "John",
                onValueChange = {},
                label = { Text("Name") },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldEmptyPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "",
                onValueChange = {},
                label = { Text("Name") },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldWithPlaceholderPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search currencies...") },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldWithPrefixPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "1500",
                onValueChange = {},
                prefix = { Text("$") },
                label = { Text("Monthly budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldMultilinePreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "Line one\nLine two",
                onValueChange = {},
                label = { Text("Notes") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF8AB4F8)
@Composable
private fun GlassTextFieldAllVariantsPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GlassTextField(
                value = "John Doe",
                onValueChange = {},
                label = { Text("Name") },
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlassTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search...") },
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlassTextField(
                value = "250.00",
                onValueChange = {},
                prefix = { Text("$") },
                label = { Text("Budget") },
            )
        }
    }
}
