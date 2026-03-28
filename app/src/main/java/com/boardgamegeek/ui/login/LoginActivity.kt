package com.boardgamegeek.ui.login

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.model.AuthToken
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    private val viewModel by viewModels<LoginViewModel>()
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null

    private var username: String? = null
    private var password: String? = null

    private lateinit var accountManager: AccountManager
    private var isRequestingNewAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.isAuthenticating.value) viewModel.cancel() else finish()
        }

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        accountManager = AccountManager.get(this)
        username = intent.getStringExtra(KEY_USERNAME)
        isRequestingNewAccount = username == null

        setContent {
            LoginRouteScreen(
                initialUsername = username,
                onLoginSuccess = { finish() },
                viewModel = viewModel,
            )
        }
    }

    private fun createAccount(authToken: AuthToken) {
        Timber.i("Creating account")
        if (authToken.username == null) return

        val account = Account(authToken.username, Authenticator.ACCOUNT_TYPE)
        try {
            accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, authToken.token)
        } catch (e: SecurityException) {
            AlertDialog.Builder(this)
                .setTitle(R.string.title_error)
                .setMessage(R.string.error_account_set_auth_token_security_exception)
                .show()
            return
        }
        val userData = bundleOf(Authenticator.KEY_AUTH_TOKEN_EXPIRY to authToken.expiry.toString())
        if (isRequestingNewAccount) {
            try {
                var success = accountManager.addAccountExplicitly(account, password, userData)
                if (!success) {
                    Authenticator.removeAccounts(applicationContext)
                    success = accountManager.addAccountExplicitly(account, password, userData)
                }
                if (!success) {
                    val accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE)
                    when {
                        accounts.isEmpty() -> return
                        accounts.size != 1 -> return
                        else -> {
                            val existingAccount = accounts[0]
                            if (existingAccount.name != account.name) return
                            accountManager.setPassword(account, password)
                        }
                    }
                }
            } catch (_: Exception) {
                return
            }
        } else {
            accountManager.setPassword(account, password)
        }
        val extras = bundleOf(
            AccountManager.KEY_ACCOUNT_NAME to authToken.username,
            AccountManager.KEY_ACCOUNT_TYPE to Authenticator.ACCOUNT_TYPE
        )
        setResult(RESULT_OK, Intent().putExtras(extras))
        accountAuthenticatorResponse?.let {
            it.onResult(extras)
            accountAuthenticatorResponse = null
        }
        preferences()[AccountPreferences.KEY_USERNAME] = authToken.username

        finish()
    }

    companion object {
        private const val KEY_USERNAME = "USERNAME"

        fun createIntentBundle(context: Context, response: AccountAuthenticatorResponse?, accountName: String?): Bundle {
            val intent = context.intentFor<LoginActivity>(
                KEY_USERNAME to accountName,
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE to response
            )
            return bundleOf(AccountManager.KEY_INTENT to intent)
        }
    }
}

@Composable
fun LoginRouteScreen(
    initialUsername: String? = null,
    onLoginSuccess: (() -> Unit)? = null,
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
                (onLoginSuccess ?: { navigator.popBackStack() })()
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
