package com.masroofi.app.presentation.components

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.masroofi.app.presentation.theme.EmeraldPrimary
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary

/**
 * نافذة الحساب السحابي الموحدة لتطبيق مصروف
 * تتيح التبديل السلس بين تبويبي "إنشاء حساب جديد" و "تسجيل الدخول"
 * مع التحقق الصارم من صحة الحقول، أمان كلمات المرور، والتصميم الزمردي الراقي
 */
@Composable
fun CloudAccountAuthSheet(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSignUp: (name: String, email: String, pass: String) -> Unit,
    onSignIn: (email: String, pass: String) -> Unit,
    onForgotPassword: (email: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: إنشاء حساب، 1: تسجيل الدخول
    val focusManager = LocalFocusManager.current

    // حقول إنشاء الحساب
    var signUpName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var isSignUpPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpConfirmVisible by remember { mutableStateOf(false) }

    // حقول تسجيل الدخول
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var isSignInPasswordVisible by remember { mutableStateOf(false) }

    // رسائل الخطأ المحلية
    var localError by remember { mutableStateOf<String?>(null) }

    // مؤشر قوة كلمة المرور
    val passwordStrength = remember(signUpPassword) {
        when {
            signUpPassword.isEmpty() -> 0
            signUpPassword.length < 6 -> 1 // ضعيفة
            signUpPassword.length < 8 -> 2 // مقبولة
            signUpPassword.any { it.isDigit() } && signUpPassword.any { it.isLetter() } -> 3 // قوية
            else -> 2
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ترويسة النافذة وزر إعدادات السحابة إن وُجد
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "السحابة",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "سحابة مصروف",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "حفظ واستعادة محفظتك بأمان بين أجهزتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // تبويبا الاختيار: "إنشاء حساب جديد" و "تسجيل الدخول"
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    contentColor = PrimaryGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            localError = null
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 0) PrimaryGreen else Color.Transparent)
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "إنشاء حساب جديد",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            localError = null
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 1) PrimaryGreen else Color.Transparent)
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // إظهار رسالة الخطأ المحلية إن وُجدت
                AnimatedVisibility(visible = localError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = localError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // محتوى التبويب الأول: إنشاء حساب جديد
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // حقل الاسم
                        OutlinedTextField(
                            value = signUpName,
                            onValueChange = {
                                signUpName = it
                                localError = null
                            },
                            label = { Text("الاسم الكامل أو اسم الحساب") },
                            placeholder = { Text("مثال: محمد علي") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = PrimaryGreen)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                focusedLabelColor = PrimaryGreen
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        // حقل البريد الإلكتروني
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = {
                                    signUpEmail = it.trim()
                                    localError = null
                                },
                                label = { Text("عنوان البريد الإلكتروني", textAlign = TextAlign.Right) },
                                placeholder = { Text("name@example.com") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Email, contentDescription = null, tint = PrimaryGreen)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // حقل كلمة المرور
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = {
                                    signUpPassword = it
                                    localError = null
                                },
                                label = { Text("كلمة المرور (6 أحرف أو أكثر)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryGreen)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isSignUpPasswordVisible = !isSignUpPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isSignUpPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isSignUpPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isSignUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // مؤشر بصري لقوة كلمة المرور
                        if (signUpPassword.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when (passwordStrength) {
                                        1 -> "ضعيفة (أقل من 6 أحرف)"
                                        2 -> "مقبولة"
                                        else -> "قوية ومحمية"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (passwordStrength) {
                                        1 -> MaterialTheme.colorScheme.error
                                        2 -> Color(0xFFE67E22)
                                        else -> PrimaryGreen
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(130.dp)
                                )

                                LinearProgressIndicator(
                                    progress = { passwordStrength / 3f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = when (passwordStrength) {
                                        1 -> MaterialTheme.colorScheme.error
                                        2 -> Color(0xFFE67E22)
                                        else -> PrimaryGreen
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        // حقل تأكيد كلمة المرور
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = signUpConfirmPassword,
                                onValueChange = {
                                    signUpConfirmPassword = it
                                    localError = null
                                },
                                label = { Text("تأكيد كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.LockReset, contentDescription = null, tint = PrimaryGreen)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isSignUpConfirmVisible = !isSignUpConfirmVisible }) {
                                        Icon(
                                            imageVector = if (isSignUpConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isSignUpConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                        }

                        // تنبيه تطابق كلمتي المرور
                        if (signUpConfirmPassword.isNotEmpty() && signUpConfirmPassword != signUpPassword) {
                            Text(
                                text = "كلمتا المرور غير متطابقتين",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // زر إنشاء الحساب
                        Button(
                            onClick = {
                                val trimmedName = signUpName.trim()
                                val trimmedEmail = signUpEmail.trim()
                                when {
                                    trimmedName.length < 2 ->
                                        localError = "يرجى إدخال اسم صحيح لا يقل عن حرفين"
                                    !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                                        localError = "صيغة البريد الإلكتروني المدخلة غير صحيحة"
                                    signUpPassword.length < 6 ->
                                        localError = "يجب ألا تقل كلمة المرور عن 6 أحرف أو أرقام"
                                    signUpPassword != signUpConfirmPassword ->
                                        localError = "كلمة المرور وتأكيدها غير متطابقين"
                                    else -> {
                                        localError = null
                                        onSignUp(trimmedName, trimmedEmail, signUpPassword)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إنشاء الحساب وربط المحفظة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                } else {
                    // محتوى التبويب الثاني: تسجيل الدخول
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // حقل البريد الإلكتروني
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = signInEmail,
                                onValueChange = {
                                    signInEmail = it.trim()
                                    localError = null
                                },
                                label = { Text("عنوان البريد الإلكتروني") },
                                placeholder = { Text("name@example.com") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Email, contentDescription = null, tint = PrimaryGreen)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // حقل كلمة المرور
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = signInPassword,
                                onValueChange = {
                                    signInPassword = it
                                    localError = null
                                },
                                label = { Text("كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryGreen)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isSignInPasswordVisible = !isSignInPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isSignInPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isSignInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                        }

                        // زر "نسيت كلمة المرور؟"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val trimmedEmail = signInEmail.trim()
                                    if (Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                        onForgotPassword(trimmedEmail)
                                    } else {
                                        localError = "يرجى كتابة بريدك الإلكتروني أولاً لإرسال رابط الاستعادة"
                                    }
                                }
                            ) {
                                Text(
                                    text = "نسيت كلمة المرور؟",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // زر تسجيل الدخول
                        Button(
                            onClick = {
                                val trimmedEmail = signInEmail.trim()
                                when {
                                    !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                                        localError = "يرجى إدخال عنوان بريد إلكتروني صحيح"
                                    signInPassword.isBlank() ->
                                        localError = "يرجى إدخال كلمة المرور"
                                    else -> {
                                        localError = null
                                        onSignIn(trimmedEmail, signInPassword)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // زر الإلغاء
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إلغاء",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * نافذة تغيير كلمة المرور للمستخدم المسجل حالياً
 */
@Composable
fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmChange: (newPassword: String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تغيير كلمة المرور",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = null },
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text("تأكيد كلمة المرور الجديدة") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                newPassword.length < 6 -> errorMsg = "كلمة المرور يجب ألا تقل عن 6 أحرف"
                                newPassword != confirmPassword -> errorMsg = "كلمتا المرور غير متطابقتين"
                                else -> onConfirmChange(newPassword)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("حفظ التغيير", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
