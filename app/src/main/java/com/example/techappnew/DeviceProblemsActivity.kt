package com.example.techappnew

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.mask.Problem
import com.example.techappnew.network.ProblemUpdateRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DeviceProblemsActivity : AppCompatActivity() {
    private var deviceId: Int = 0
    private val problems = mutableListOf<Problem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_problems)

        deviceId = intent.getIntExtra("device_id", 0)
        if (deviceId == 0) {
            Toast.makeText(this, "Неверный ID устройства", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.toolbar).setOnClickListener { finish() }
        loadProblems()
    }

    private fun loadProblems() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.problemsApi.getProblems(deviceId = deviceId)
                if (response.isSuccessful) {
                    response.body()?.let { problemsList ->
                        problems.clear()
                        problems.addAll(problemsList)
                        updateProblemsUI()
                    }
                } else {
                    showError("Ошибка загрузки проблем: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun updateProblemsUI() {
        val problemsContainer = findViewById<LinearLayout>(R.id.problemsContainer)
        problemsContainer.removeAllViews()

        if (problems.isEmpty()) {
            problemsContainer.addView(
                TextView(this).apply {
                    text = "Проблемы не найдены"
                    setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
                    gravity = Gravity.CENTER
                    setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx()) // Добавляем отступы
                }
            )
            return
        }

        problems.forEach { problem ->
            val problemView = MaterialButton(this, null,
                com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton).apply {
                text = problem.description ?: "Проблема ${problem.problem_id}"
                setTextColor(ContextCompat.getColor(context, R.color.primary_color))
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                strokeColor = ContextCompat.getColorStateList(context, R.color.primary_color)
                strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width)

                // Добавляем отступы для текста
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())

                // Добавляем внешние отступы для карточки проблемы
                val margin = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }
                layoutParams = margin

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8.dpToPx(), 0, 0) // Отступ между кнопками и описанием
                    }
                }

                val deleteBtn = MaterialButton(context).apply {
                    text = "Удалить"
                    setTextColor(ContextCompat.getColor(context, R.color.error_red))
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                    strokeColor = ContextCompat.getColorStateList(context, R.color.error_red)
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width) // Обводка
                    setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx()) // Отступы текста
                    setOnClickListener { deleteProblem(problem.problem_id) }
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(4.dpToPx(), 0, 4.dpToPx(), 0) // Отступы между кнопками
                    }
                }

                val statusBtn = MaterialButton(context).apply {
                    text = when (problem.status) {
                        "Pending" -> "В работу"
                        "In Progress" -> "Решено"
                        "Resolved" -> "В ожидание"
                        else -> problem.status
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.primary_color))
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                    strokeColor = ContextCompat.getColorStateList(context, R.color.primary_color)
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width) // Обводка
                    setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx()) // Отступы текста
                    setOnClickListener { toggleProblemStatus(problem) }
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(4.dpToPx(), 0, 4.dpToPx(), 0) // Отступы между кнопками
                    }
                }

                layout.addView(deleteBtn)
                layout.addView(statusBtn)
                problemsContainer.addView(layout)

                setOnClickListener {
                    showProblemDetails(problem)
                }
            }
            problemsContainer.addView(problemView)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun deleteProblem(problemId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.problemsApi.deleteProblem(problemId)
                if (response.isSuccessful) {
                    showToast("Проблема удалена")
                    loadProblems()
                } else {
                    showError("Ошибка удаления: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun toggleProblemStatus(problem: Problem) {
        lifecycleScope.launch {
            try {
                val newStatus = when (problem.status) {
                    "Pending" -> "In Progress"
                    "In Progress" -> "Resolved"
                    "Resolved" -> "Pending"
                    else -> "Pending"
                }

                val response = RetrofitInstance.problemsApi.updateProblem(
                    problem.problem_id,
                    ProblemUpdateRequest(status = newStatus)
                )

                if (response.isSuccessful) {
                    showToast("Статус обновлен: $newStatus")
                    loadProblems()
                } else {
                    showError("Ошибка обновления: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun showProblemDetails(problem: Problem) {
        Toast.makeText(this,
            "Проблема: ${problem.description}\nСтатус: ${problem.status}",
            Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}