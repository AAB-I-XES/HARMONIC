package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    var isLoginMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Premium styling parameters adapting to current dark/light state
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceContainerColor = if (isDarkMode) Color(0xFF1E1C24) else Color(0xFFF3EDF7)
    val inputBgColor = if (isDarkMode) Color(0xFF121118) else Color.White
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)
    val textSecondaryColor = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F)

    // Glowing themed background
    val ambientGradient = Brush.verticalGradient(
        colors = if (isDarkMode) {
            listOf(
                accentColor.copy(alpha = 0.25f),
                Color(0xFF0F0E13)
            )
        } else {
            listOf(
                accentColor.copy(alpha = 0.15f),
                Color(0xFFFEF7FF)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ambientGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-fidelity header toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(
                            if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                            RoundedCornerShape(12.dp)
                        )
                        .size(40.dp)
                        .testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = textPrimaryColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Brand Premium Identity Icon (Glowing Waveforms style shell)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Musical note stylized presentation
                Row(
                    modifier = Modifier.height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(0.4f, 0.9f, 0.6f, 0.3f, 0.8f).forEach { scale ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight(scale)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Redesigned Welcoming Banner
            Text(
                text = if (isLoginMode) "Welcome Back" else "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isLoginMode) 
                    "Log in to sync your smart mixes and play offline" 
                else 
                    "Join to craft personalized AI-curated radio playlists",
                fontSize = 13.sp,
                color = textSecondaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Premium input console with soft border edge depth
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .testTag("auth_input_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Display Name Box - Animated Reveal during Register mode
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { 
                                usernameInput = it
                                errorMessage = null
                            },
                            placeholder = { Text("Display Username", color = textSecondaryColor) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimaryColor,
                                unfocusedTextColor = textPrimaryColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_username_field")
                        )
                    }

                    // Email Box
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("Email Address", color = textSecondaryColor) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = accentColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field")
                    )

                    // Password Box
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { 
                            passwordInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("Enter Password", color = textSecondaryColor) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "🙈" else "👁️", fontSize = 16.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field")
                    )

                    // Dynamic error reports
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Solid Core Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            
                            if (emailInput.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                errorMessage = "Please enter a valid email address."
                                return@Button
                            }
                            if (passwordInput.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (!isLoginMode && usernameInput.isBlank()) {
                                errorMessage = "Please enter your Username."
                                return@Button
                            }

                            coroutineScope.launch {
                                isSubmitting = true
                                delay(1200) // Beautiful network latency simulation
                                isSubmitting = false
                                
                                val resolvedUsername = if (isLoginMode) {
                                    emailInput.substringBefore("@").replaceFirstChar { it.lowercase() }
                                } else {
                                    usernameInput
                                }
                                
                                viewModel.loginUser(emailInput, resolvedUsername)
                                onBackClick()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Log In" else "Create Account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Redesigned interactive switcher to toggle between flows with subtle feedback lines
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        isLoginMode = !isLoginMode
                        errorMessage = null
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("auth_toggle_mode")
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                    color = textSecondaryColor,
                    fontSize = 13.sp
                )
                Text(
                    text = if (isLoginMode) "Sign Up" else "Log In",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(0.4f))
        }
    }
}
