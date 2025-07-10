package com.example.techappnew

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techappnew.network.ProblemRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.IOException

class RoomsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        setContentView(R.layout.activity_rooms)

        showSkeleton()

        val buildingName = intent.getStringExtra("building_name") ?: ""
        val roomsList = intent.getStringArrayListExtra("rooms_list") ?: arrayListOf()
        val userId = intent.getIntExtra("user_id", 0)

        // Симуляция загрузки данных
        loadData(buildingName, roomsList)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
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



    private fun loadData(buildingName: String, roomsList: ArrayList<String>) {
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<TextView>(R.id.titleTextView).text = buildingName

            val recyclerView = findViewById<RecyclerView>(R.id.roomsRecyclerView)
            recyclerView.layoutManager = GridLayoutManager(this, 2)

            val roomsIds = intent.getIntegerArrayListExtra("rooms_ids") ?: arrayListOf()
            val roomsWithIds = roomsList.zip(roomsIds)

            recyclerView.adapter = RoomsAdapter(roomsList) { roomName ->
                val roomNumber = roomName.replace("N°", "").toIntOrNull() ?: 0
                val classroomId = roomsWithIds.firstOrNull { it.first == roomName }?.second ?: 0
                val intent = Intent(this, ClassActivity::class.java).apply {
                    putExtra("classroom_id", classroomId)
                    putExtra("classroom_number", roomNumber)
                    putExtra("building_name", buildingName)
                    putExtra("is_admin", false)
                    putExtra("user_id", intent.getIntExtra("user_id", 0))
                }
                startActivity(intent)
            }

            hideSkeleton()
        }, 1000)
    }

    private fun showSkeleton() {
        // Показываем скелетон и скрываем основной контент
        findViewById<View>(R.id.skeletonContainer).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.mainContent).visibility = View.INVISIBLE
        findViewById<RecyclerView>(R.id.roomsRecyclerView).visibility = View.INVISIBLE

        // Создаем список всех скелетон-элементов
        val skeletonViews = listOf(
            // Основные блоки
            findViewById<View>(R.id.skeletonSearchBar),
            findViewById<View>(R.id.skeletonTitle),

            // Кнопки фильтров
            findViewById<View>(R.id.skeletonFilterButton1),
            findViewById<View>(R.id.skeletonFilterButton2),
            findViewById<View>(R.id.skeletonFilterButton3),

            // Аудитории (комнаты)
            findViewById<View>(R.id.skeletonRoom1),
            findViewById<View>(R.id.skeletonRoom2),
            findViewById<View>(R.id.skeletonRoom3),
            findViewById<View>(R.id.skeletonRoom4),
            findViewById<View>(R.id.skeletonRoom5)
        )

        // Настройка анимации для каждого элемента
        skeletonViews.forEach { view ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.shimmer_animation).apply {
                interpolator = AccelerateDecelerateInterpolator()
                startOffset = 0
            }
            view.startAnimation(anim)
        }
    }

    private fun hideSkeleton() {
        val skeletonContainer = findViewById<View>(R.id.skeletonContainer)
        skeletonContainer.clearAnimation()
        skeletonContainer.visibility = View.GONE

        findViewById<LinearLayout>(R.id.mainContent).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.roomsRecyclerView).visibility = View.VISIBLE
    }
}