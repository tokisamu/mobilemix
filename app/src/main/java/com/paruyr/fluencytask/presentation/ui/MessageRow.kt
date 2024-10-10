package com.paruyr.fluencytask.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paruyr.fluencytask.domain.model.FluencyMessage

@Composable
fun MessageRow(message: FluencyMessage) {
    val alignment = if (message.isSent) Alignment.TopEnd else Alignment.TopStart
    Box(
        contentAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = message.content,
            color = if (message.isSent) Color.Blue else Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}