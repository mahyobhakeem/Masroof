package com.masroofi.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.R

/**
 * شاشة إقلاع احترافية (Splash Screen) بهوية التطبيق الخضراء الزمردية الفاخرة
 * تمنع ظهور الشاشة البيضاء المؤقتة وتوفر تجربة بدء تشغيل انسيابية وناعمة
 */
@Composable
fun MasroofSplashScreen(
    modifier: Modifier = Modifier
) {
    val emeraldGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF106F4D),
            Color(0xFF0E6B4E),
            Color(0xFF094E38)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(emeraldGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // أيقونة التطبيق الرسمية بحجم واضح وأنيق
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_masroof_logo),
                    contentDescription = "شعار مصروف",
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // اسم التطبيق الرسمي
            Text(
                text = "مصروف",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "رفيقك المالي الذكي لإدارة النفقات",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // دائرة انتظار احترافية ناعمة تدور بلون أبيض زمردي
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                strokeWidth = 3.dp,
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // نص التجهيز الصغير
            Text(
                text = "جاري تجهيز محفظتك المالية...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
        }
    }
}
