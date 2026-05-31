package com.example.scraply.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.scraply.R

/**
 * Gates [content] behind authentication. Shows either:
 *  - a "Firebase not configured" notice if the project's `google-services.json` is missing, or
 *  - an email/password + Google sign-in pane if the user is not signed in, or
 *  - [content] once the user is authenticated.
 *
 * Used by both the Feed and Profile tabs so the login experience is identical in both places.
 */
@Composable
fun AuthGate(
    vm: BaseAuthViewModel,
    signedOutHeadline: String,
    signedOutSubtext: String,
    content: @Composable () -> Unit,
) {
    val state by vm.authState.collectAsState()
    val ctx = LocalContext.current
    when {
        !state.firebaseAvailable -> NotConfiguredState()
        !state.authReady -> AuthResolvingState()
        !state.isSignedIn -> AuthPane(
            state = state,
            headline = signedOutHeadline,
            subtext = signedOutSubtext,
            onSubmitEmail = { e, p, n -> vm.submitEmail(e, p, n) },
            onGoogle = { vm.signInWithGoogle(ctx) },
            onToggleMode = { vm.toggleAuthMode() },
            onForgotPassword = { vm.sendPasswordReset(it) },
            onDismissError = { vm.dismissError() },
        )
        else -> content()
    }
}

@Composable
private fun AuthResolvingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotConfiguredState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Public, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.auth_firebase_not_configured), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.auth_firebase_not_configured_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthPane(
    state: AuthUiState,
    headline: String,
    subtext: String,
    onSubmitEmail: (email: String, password: String, displayName: String?) -> Unit,
    onGoogle: () -> Unit,
    onToggleMode: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onDismissError: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isSignUp = state.authMode == AuthMode.SignUp
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Public, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isSignUp) stringResource(R.string.auth_create_account_headline) else headline,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (isSignUp)
                stringResource(R.string.auth_create_account_subtext)
            else subtext,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onDismissError() },
                label = { Text(stringResource(R.string.auth_display_name_label)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onDismissError() },
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onDismissError() },
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isSignUp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onForgotPassword(email) }) { Text(stringResource(R.string.auth_forgot_password)) }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.info != null) {
            Spacer(Modifier.height(6.dp))
            Text(state.info, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onSubmitEmail(email.trim(), password, name.takeIf { it.isNotBlank() }) },
            enabled = !state.loading && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (isSignUp) stringResource(R.string.auth_create_account_btn) else stringResource(R.string.auth_sign_in_btn))
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("  ${stringResource(R.string.auth_or_divider)}  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onGoogle,
            enabled = !state.loading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            GoogleMark(size = 18.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                if (isSignUp) stringResource(R.string.auth_signup_google) else stringResource(R.string.auth_signin_google),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isSignUp) stringResource(R.string.auth_already_have_account) else stringResource(R.string.auth_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onToggleMode) { Text(if (isSignUp) stringResource(R.string.auth_sign_in_btn) else stringResource(R.string.auth_sign_up_btn)) }
        }
    }
}

@Composable
private fun GoogleMark(size: Dp) {
    Box(
        modifier = Modifier.size(size).background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}
