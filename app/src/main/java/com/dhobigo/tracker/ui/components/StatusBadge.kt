package com.dhobigo.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhobigo.tracker.data.local.model.LaundryStatus
import com.dhobigo.tracker.ui.theme.StatusCollectedBg
import com.dhobigo.tracker.ui.theme.StatusCollectedBorder
import com.dhobigo.tracker.ui.theme.StatusCollectedText
import com.dhobigo.tracker.ui.theme.StatusPartialBg
import com.dhobigo.tracker.ui.theme.StatusPartialBorder
import com.dhobigo.tracker.ui.theme.StatusPartialText
import com.dhobigo.tracker.ui.theme.StatusPendingBg
import com.dhobigo.tracker.ui.theme.StatusPendingBorder
import com.dhobigo.tracker.ui.theme.StatusPendingText

@Composable
fun StatusBadge(
    status: LaundryStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (status) {
        LaundryStatus.PENDING -> Triple(StatusPendingBg, StatusPendingBorder, StatusPendingText)
        LaundryStatus.PARTIALLY_RETURNED -> Triple(StatusPartialBg, StatusPartialBorder, StatusPartialText)
        LaundryStatus.COLLECTED -> Triple(StatusCollectedBg, StatusCollectedBorder, StatusCollectedText)
    }

    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}
