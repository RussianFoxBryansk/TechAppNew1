package com.example.techappnew

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.network.AddRequest
import com.example.techappnew.network.LoginPOST
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.HttpException as HttpException1

class AuthActivity : AppCompatActivity() {

    private lateinit var userEmail: TextInputEditText
    private lateinit var userLogin: TextInputEditText
    private lateinit var userPass: TextInputEditText
    private lateinit var CreateAccount: Button
    private lateinit var btnLogin: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        userEmail = findViewById(R.id.etemail)
        userLogin = findViewById(R.id.etlogin)
        userPass = findViewById(R.id.etpass)
        CreateAccount = findViewById(R.id.btnCreateAccount)
        btnLogin = findViewById(R.id.btnLogin)

        CreateAccount.setOnClickListener{
            val email = userEmail.text.toString().trim()
            val login = userLogin.text.toString().trim()
            val pass = userPass.text.toString().trim()
            if (email.isEmpty() || login.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            } else if (!email.contains("@")) {
                Toast.makeText(this, "Email должен содержать символ @", Toast.LENGTH_LONG).show()
            }else {
                lifecycleScope.launch {
                    try {
                        val loginRequest =
                            AddRequest(email = email, login = login, password = pass, admin = false)
                        val response: User =
                            RetrofitInstance.userApi.regUser(loginRequest)
                        runOnUiThread {
                            Toast.makeText(this@AuthActivity, "Аккаунт успешно создан!", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finish()
                        }
                    } catch (e: HttpException1) {
                        Log.e("LoginActivity", "Ошибка при выполнении запроса")
                        runOnUiThread {
                            Toast.makeText(this@AuthActivity, "Ошибка сети", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Ошибка: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@AuthActivity, "${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }


        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }


}