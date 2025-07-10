package com.example.techappnew

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techappnew.network.CumpusRequest
import com.example.techappnew.network.ProblemRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.IOException

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var buildingsRecycler: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var addButton: FloatingActionButton
    private lateinit var skeletonContainer: View
    private lateinit var sharedPreferences: SharedPreferences
    private var userId: Int = 0
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        setContentView(R.layout.activity_home)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Получение данных пользователя из SharedPreferences
        userId = sharedPreferences.getInt("user_id", 0)
        isAdmin = sharedPreferences.getBoolean("is_admin", false)

        if (userId == 0) {
            // Если ID пользователя нет, возвращаемся на экран авторизации
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Инициализация элементов UI
        buildingsRecycler = findViewById(R.id.buildingsRecycler)
        bottomNav = findViewById(R.id.bottomNav)
        addButton = findViewById(R.id.addButton)
        skeletonContainer = findViewById(R.id.skeletonContainer)

        // Показать скелетон при запуске
        showSkeleton()

        // Настройка нижней навигации
        setupNavigation()

        // Загрузка данных из API
        loadBuildingsFromApi()

        setupAddButton()
    }

    private fun setupNavigation() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
                    false
                }
                else -> false
            }
        }
    }

    private fun loadBuildingsFromApi() {
        lifecycleScope.launch {
            try {
                Log.d("HomeActivity", "Starting API requests...")

                val response = RetrofitInstance.cumpusApi.getCumpusList()
                Log.d("HomeActivity", "Cumpus response code: ${response.code()}")

                if (response.isSuccessful) {
                    val cumpusList = response.body() ?: emptyList()
                    Log.d("HomeActivity", "Received ${cumpusList.size} campuses:")

                    val roomsResponse = RetrofitInstance.roomsApi.getAllRooms()
                    Log.d("HomeActivity", "Rooms response code: ${roomsResponse.code()}")

                    val allRooms = roomsResponse.body() ?: emptyList()

                    val buildings = cumpusList.map { cumpus ->
                        val campusRooms = allRooms
                            .filter { it.campus_id == cumpus.campus_id }
                            .map { classroom ->
                                BuildingsAdapter.Room(
                                    classroomId = classroom.classroom_id,
                                    classroomNumber = classroom.classroom_number,
                                    campusId = classroom.campus_id
                                )
                            }

                        BuildingsAdapter.Building(
                            name = cumpus.address ?: "Корпус ${cumpus.campus_number}",
                            rooms = campusRooms,
                            buildingId = cumpus.campus_id.toString()
                        )
                    }

                    runOnUiThread {
                        setupBuildingsRecycler(buildings)
                        hideSkeleton()
                    }
                } else {
                    throw Exception("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading data", e)
                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        "Ошибка загрузки данных: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideSkeleton()
                }
            }
        }
    }

    private fun setupBuildingsRecycler(buildings: List<BuildingsAdapter.Building>) {
        buildingsRecycler.layoutManager = LinearLayoutManager(this)
        buildingsRecycler.adapter = BuildingsAdapter(buildings) { roomName, buildingName, classroomId ->
            val roomNumber = roomName.replace("N°", "").toIntOrNull() ?: 0
            val intent = Intent(this, ClassActivity::class.java).apply {
                putExtra("classroom_id", classroomId)
                putExtra("classroom_number", roomNumber)
                putExtra("building_name", buildingName)
                putExtra("is_admin", isAdmin)
            }
            startActivity(intent)
        }
    }

    private fun showSkeleton() {
        skeletonContainer.visibility = View.VISIBLE
        buildingsRecycler.visibility = View.INVISIBLE
        addButton.visibility = View.INVISIBLE

        val skeletonViews = listOf(
            findViewById<View>(R.id.skeletonSearchBar),
            findViewById<View>(R.id.skeletonTitle),
            findViewById<View>(R.id.skeletonFilterButton1),
            findViewById<View>(R.id.skeletonFilterButton2),
            findViewById<View>(R.id.skeletonFilterButton3),
            findViewById<View>(R.id.skeletonRoom1),
            findViewById<View>(R.id.skeletonRoom2),
            findViewById<View>(R.id.skeletonRoom3),
            findViewById<View>(R.id.skeletonRoom4),
            findViewById<View>(R.id.skeletonRoom5),
            findViewById<View>(R.id.skeletonBottomNav)
        )

        skeletonViews.forEach { view ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.shimmer_animation).apply {
                interpolator = AccelerateDecelerateInterpolator()
                startOffset = 0
            }
            view.startAnimation(anim)
            view.postDelayed({ view.isActivated = !view.isActivated }, 800)
        }
    }

    private fun hideSkeleton() {
        skeletonContainer.clearAnimation()
        skeletonContainer.visibility = View.GONE
        buildingsRecycler.visibility = View.VISIBLE
        addButton.visibility = View.VISIBLE
    }

    private fun setupAddButton() {
        addButton.setOnClickListener {
            showProblemDialog()
        }
    }

    private fun showProblemDialog() {
        lifecycleScope.launch {
            try {
                val campuses = RetrofitInstance.cumpusApi.getCumpusList().body() ?: emptyList()

                val dialog = ProblemDialog(
                    context = this@HomeActivity,
                    userId = userId,
                    campuses = campuses,
                    roomsApi = RetrofitInstance.roomsApi,
                    placesApi = RetrofitInstance.placesApi,
                    devicesApi = RetrofitInstance.devicesApi,
                    onProblemCreated = ::createProblem,
                    lifecycle = lifecycle
                )

                dialog.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                dialog.show()
            }  catch (e: Exception) {
                Log.e("HomeActivity", "Error showing dialog", e)
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка открытия диалога", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createProblem(problemRequest: ProblemRequest) {
        lifecycleScope.launch {
            try {
                if (problemRequest.user_id == 0) {
                    throw IllegalArgumentException("User ID не может быть 0")
                }
                if (problemRequest.device_id == 0) {
                    throw IllegalArgumentException("Device ID не может быть 0")
                }
                if (problemRequest.description.isBlank()) {
                    throw IllegalArgumentException("Описание не может быть пустым")
                }

                val response = RetrofitInstance.problemsApi.createProblem(problemRequest)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Нет описания ошибки"
                    throw IOException("Ошибка сервера ${response.code()}: $errorBody")
                }

                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        "Проблема успешно создана",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is IllegalArgumentException -> "Ошибка данных: ${e.message}"
                    is IOException -> "Сетевая ошибка: ${e.message}"
                    else -> "Неизвестная ошибка: ${e.message}"
                }

                Log.e("ProblemCreation", errorMsg, e)
                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}