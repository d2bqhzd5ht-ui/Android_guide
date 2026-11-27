package com.example.scenicguide.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.rememberAppContainer

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.factory(container.authRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "西湖导览",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "WEST LAKE",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "创建账号，开启西湖之旅",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )

        GuideCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("账号（3 位以上）", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密码（6 位以上）", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("确认密码", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("邮箱（可选）", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = viewModel::register,
                enabled = !state.loading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("注册")
                }
            }

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "已有账号？返回登录",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
