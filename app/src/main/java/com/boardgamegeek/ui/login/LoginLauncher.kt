package com.boardgamegeek.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.model.AuthToken
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.theme.AppTheme
import timber.log.Timber

object LoginLauncher {
    fun createIntentBundle(
        context: Context,
        response: android.accounts.AccountAuthenticatorResponse?,
        accountName: String?,
    ): android.os.Bundle {
        val intent = MainActivity.createIntent(
            context = context,
            route = LoginRoute(username = accountName),
            replaceBackStack = true,
        ).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        }
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }
}

@Composable
fun LoginRouteScreen(
    initialUsername: String? = null,
    onLoginSuccess: ((String) -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authenticationResult by viewModel.authenticationResult.collectAsStateWithLifecycle()
    var usernameInput by rememberSaveable { mutableStateOf(initialUsername.orEmpty()) }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var usernameError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var attemptedLogin by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(authenticationResult, isAuthenticating, attemptedLogin) {
        if (!isAuthenticating && attemptedLogin) {
            val result = authenticationResult
            if (result == null) {
                passwordError = context.getString(R.string.error_incorrect_password)
            } else if (completeLogin(context, result, passwordInput, initialUsername == null)) {
                val username = result.username
                if (username != null) {
                    onLoginSuccess?.invoke(username) ?: navigator.popBackStack()
                } else {
                    context.toast(R.string.title_error)
                }
            } else {
                context.toast(R.string.title_error)
            }
            attemptedLogin = false
        }
    }

    AppTheme {
        LoginScreen(
            isRequestingNewAccount = initialUsername == null,
            username = usernameInput,
            password = passwordInput,
            usernameError = usernameError,
            passwordError = passwordError,
            isAuthenticating = isAuthenticating,
            onUsernameChanged = {
                usernameInput = it
                usernameError = null
            },
            onPasswordChanged = {
                passwordInput = it
                passwordError = null
            },
            onSignIn = {
                usernameError = null
                passwordError = null

                val trimmedUsername = usernameInput.trim()
                when {
                    trimmedUsername.isBlank() -> usernameError = context.getString(R.string.error_field_required)
                    passwordInput.isBlank() -> passwordError = context.getString(R.string.error_field_required)
                    else -> {
                        keyboard?.hide()
                        attemptedLogin = true
                        viewModel.clearAuthenticationResult()
                        viewModel.login(trimmedUsername, passwordInput)
                    }
                }
            },
        )
    }
}

private fun completeLogin(
    context: Context,
    authToken: AuthToken,
    password: String,
    isRequestingNewAccount: Boolean,
): Boolean {
    val username = authToken.username ?: return false
    Timber.i("Creating account")
    val accountManager = AccountManager.get(context)
    val account = Account(username, Authenticator.ACCOUNT_TYPE)
    return try {
        accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, authToken.token)
        val userData = bundleOf(Authenticator.KEY_AUTH_TOKEN_EXPIRY to authToken.expiry.toString())
        if (isRequestingNewAccount) {
            var success = accountManager.addAccountExplicitly(account, password, userData)
            if (!success) {
                Authenticator.removeAccounts(context.applicationContext)
                success = accountManager.addAccountExplicitly(account, password, userData)
            }
            if (!success) {
                val accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE)
                when {
                    accounts.isEmpty() -> return false
                    accounts.size != 1 -> return false
                    else -> {
                        val existingAccount = accounts[0]
                        if (existingAccount.name != account.name) return false
                        accountManager.setPassword(account, password)
                    }
                }
            }
        } else {
            accountManager.setPassword(account, password)
        }
        context.preferences()[AccountPreferences.KEY_USERNAME] = username
        true
    } catch (_: Exception) {
        false
    }
}

@androidx.compose.runtime.Composable
private fun LoginScreen(
    isRequestingNewAccount: Boolean,
    username: String,
    password: String,
    usernameError: String?,
    passwordError: String?,
    isAuthenticating: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isAuthenticating) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.login_progress_signing_in))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.prompt_username)) },
                    enabled = isRequestingNewAccount,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = !usernameError.isNullOrBlank(),
                    supportingText = {
                        if (!usernameError.isNullOrBlank()) {
                            Text(usernameError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.prompt_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSignIn() }),
                    isError = !passwordError.isNullOrBlank(),
                    supportingText = {
                        if (!passwordError.isNullOrBlank()) {
                            Text(passwordError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.action_sign_in))
                }
            }
        }
    }
}
