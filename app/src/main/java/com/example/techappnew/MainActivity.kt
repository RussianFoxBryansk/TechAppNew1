package com.example.techappnew

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.network.LoginRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    private lateinit var userLogin: TextInputEditText
    private lateinit var userPass: TextInputEditText
    private lateinit var rePass: TextView
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Проверка сохраненного ID пользователя
        val savedUserId = sharedPreferences.getInt("user_id", 0)
        if (savedUserId != 0) {
            // Если есть сохраненный ID, переходим сразу в HomeActivity
            navigateToHomeActivity(
                savedUserId,
                sharedPreferences.getString("user_login", "") ?: "",
                sharedPreferences.getString("user_email", "") ?: "",
                sharedPreferences.getBoolean("is_admin", false)
            )
            return
        }

        // Инициализация UI элементов
        userLogin = findViewById(R.id.etlogin)
        userPass = findViewById(R.id.etpass)
        rePass = findViewById(R.id.tvrepass)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        rePass.setOnClickListener {
            showPasswordRecoveryDialog()
        }

        loginButton.setOnClickListener {
            val login = userLogin.text.toString().trim()
            val pass = userPass.text.toString().trim()

            Log.d("AuthFlow", "Начало авторизации. Логин: '$login'")

            lifecycleScope.launch {
                try {
                    val authResponse = RetrofitInstance.userApi.loginUser(LoginRequest(login, pass))

                    if (!authResponse.isSuccessful) {
                        throw Exception("HTTP ${authResponse.code()}")
                    }

                    val authData = authResponse.body() ?: throw Exception("Пустой ответ")

                    if (!authData.success) {
                        throw Exception(authData.message ?: "Auth failed")
                    }

                    val userResponse = RetrofitInstance.userApi.getUserByLogin(login)

                    if (!userResponse.isSuccessful) {
                        throw Exception("HTTP ${userResponse.code()}")
                    }

                    val user = userResponse.body() ?: throw Exception("Нет данных пользователя")

                    if (user.user_id != authData.user_id) {
                        throw Exception("Несоответствие ID")
                    }

                    // Сохраняем данные пользователя в SharedPreferences
                    sharedPreferences.edit {
                        putInt("user_id", user.user_id)
                        putString("user_login", user.login)
                        putString("user_email", user.email)
                        putBoolean("is_admin", user.admin)
                        apply()
                    }

                    navigateToHomeActivity(user.user_id, user.login, user.email, user.admin)

                } catch (e: Exception) {
                    Log.e("AuthError", "Ошибка авторизации", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            when {
                                e.message?.contains("HTTP") == true -> "Ошибка сервера"
                                e.message?.contains("Несоответствие") == true -> "Ошибка данных"
                                else -> "Ошибка авторизации"
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
        }
    }

    private fun navigateToHomeActivity(userId: Int, login: String, email: String, isAdmin: Boolean) {
        startActivity(Intent(this@MainActivity, HomeActivity::class.java).apply {
            putExtra("user_id", userId)
            putExtra("user_login", login)
            putExtra("user_email", email)
            putExtra("is_admin", isAdmin)
        })
        finish()
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordRecoveryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_recovery, null)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.etRecoveryEmail)
        val recoverButton = dialogView.findViewById<Button>(R.id.btnRecover)

        val dialog = AlertDialog.Builder(this, R.style.RoundedDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        recoverButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendPassword(email)
                dialog.dismiss()
            } else {
                emailInput.error = "Введите корректный email"
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sendPassword(email: String) {
        lifecycleScope.launch {
            try {
                RetrofitInstance.userApi.sendPasswor(email)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Инструкции по восстановлению отправлены на $email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                Log.e("PasswordRecovery", "HTTP error: ${e.code()}")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка сервера: ${e.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PasswordRecovery", "Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка: ${e.message ?: "Неизвестная ошибка"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}