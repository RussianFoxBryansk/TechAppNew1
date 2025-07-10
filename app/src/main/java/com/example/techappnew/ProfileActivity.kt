package com.example.techappnew

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.network.ClassroomRequest
import com.example.techappnew.network.CumpusRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ProfileActivity : AppCompatActivity() {
    private var campuses: List<com.example.techappnew.mask.Cumpus> = emptyList()
    private var allRooms: List<ClassroomRequest> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        setContentView(R.layout.activity_profile)

        // Настройка нижней навигации
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        // Обработчики кнопок
        findViewById<MaterialButton>(R.id.ComputerRoomBtn).setOnClickListener {
            showTechSetupDialog()
        }
        findViewById<MaterialButton>(R.id.addBuildingBtn).setOnClickListener {
            showAddCampusDialog()
        }
        findViewById<MaterialButton>(R.id.deleteBuildingBtn).setOnClickListener {
            showDeleteCampusDialog()
        }
        findViewById<MaterialButton>(R.id.addRoomBtn).setOnClickListener {
            showAddClassroomDialog()
        }
        findViewById<MaterialButton>(R.id.deleteRoomBtn).setOnClickListener {
            showDeleteClassroomDialog()
        }
    }

    private fun showAddCampusDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_campus)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val campusNumberInput = dialog.findViewById<EditText>(R.id.campusNumberInput)
        val addressInput = dialog.findViewById<EditText>(R.id.addressInput)
        val confirmBtn = dialog.findViewById<MaterialButton>(R.id.confirmBtn)

        confirmBtn.setOnClickListener {
            val campusNumber = campusNumberInput.text.toString().toIntOrNull()
            val address = addressInput.text.toString()

            if (campusNumber == null || address.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.cumpusApi.createCampus(
                        CumpusRequest(
                            campus_id = 0, // ID будет сгенерирован на сервере
                            campus_number = campusNumber,
                            address = address
                        )
                    )

                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Корпус успешно добавлен",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                    } else {
                        throw Exception("Ошибка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteCampusDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_campus)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val campusSpinner = dialog.findViewById<Spinner>(R.id.campusSpinner)
        val confirmBtn = dialog.findViewById<MaterialButton>(R.id.confirmBtn)

        // Загрузка списка корпусов
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.cumpusApi.getCumpusList()
                if (response.isSuccessful) {
                    campuses = response.body() ?: emptyList()
                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@ProfileActivity,
                            android.R.layout.simple_spinner_item,
                            campuses.map { it.address ?: "Корпус ${it.campus_number}" }
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        campusSpinner.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки корпусов: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        confirmBtn.setOnClickListener {
            val selectedPosition = campusSpinner.selectedItemPosition
            if (selectedPosition >= 0 && selectedPosition < campuses.size) {
                val campusId = campuses[selectedPosition].campus_id

                lifecycleScope.launch {
                    try {
                        val response = RetrofitInstance.cumpusApi.deleteCampus(campusId)
                        if (response.isSuccessful) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Корпус успешно удален",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            }
                        } else {
                            throw Exception("Ошибка сервера: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Ошибка: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showAddClassroomDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_classroom)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val classroomNumberInput = dialog.findViewById<EditText>(R.id.classroomNumberInput)
        val campusSpinner = dialog.findViewById<Spinner>(R.id.campusSpinner)
        val confirmBtn = dialog.findViewById<MaterialButton>(R.id.confirmBtn)

        // Загрузка списка корпусов
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.cumpusApi.getCumpusList()
                if (response.isSuccessful) {
                    campuses = response.body() ?: emptyList()
                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@ProfileActivity,
                            android.R.layout.simple_spinner_item,
                            campuses.map { it.address ?: "Корпус ${it.campus_number}" }
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        campusSpinner.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки корпусов: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        confirmBtn.setOnClickListener {
            val classroomNumber = classroomNumberInput.text.toString().toIntOrNull()
            val campusPosition = campusSpinner.selectedItemPosition

            if (classroomNumber == null || campusPosition < 0 || campusPosition >= campuses.size) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val campusId = campuses[campusPosition].campus_id

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.roomsApi.createClassroom(
                        ClassroomRequest(
                            classroom_id = 0, // ID будет сгенерирован на сервере
                            classroom_number = classroomNumber,
                            campus_id = campusId
                        )
                    )

                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Аудитория успешно добавлена",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                    } else {
                        throw Exception("Ошибка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteClassroomDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_classroom)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val campusSpinner = dialog.findViewById<Spinner>(R.id.campusSpinner)
        val classroomSpinner = dialog.findViewById<Spinner>(R.id.classroomSpinner)
        val confirmBtn = dialog.findViewById<MaterialButton>(R.id.confirmBtn)

        // Загрузка списка корпусов
        lifecycleScope.launch {
            try {
                val campusResponse = RetrofitInstance.cumpusApi.getCumpusList()
                val roomsResponse = RetrofitInstance.roomsApi.getAllRooms()

                if (campusResponse.isSuccessful && roomsResponse.isSuccessful) {
                    campuses = campusResponse.body() ?: emptyList()
                    allRooms = roomsResponse.body() ?: emptyList()

                    runOnUiThread {
                        // Настройка спиннера корпусов
                        val campusAdapter = ArrayAdapter(
                            this@ProfileActivity,
                            android.R.layout.simple_spinner_item,
                            campuses.map { it.address ?: "Корпус ${it.campus_number}" }
                        )
                        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        campusSpinner.adapter = campusAdapter

                        // Обновление списка аудиторий при выборе корпуса
                        campusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (position >= 0 && position < campuses.size) {
                                    updateClassroomsSpinner(campuses[position].campus_id, classroomSpinner)
                                }
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                        }

                        // Инициализация для первого корпуса
                        if (campuses.isNotEmpty()) {
                            updateClassroomsSpinner(campuses[0].campus_id, classroomSpinner)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки данных: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        confirmBtn.setOnClickListener {
            val campusPosition = campusSpinner.selectedItemPosition
            val classroomPosition = classroomSpinner.selectedItemPosition

            if (campusPosition >= 0 && classroomPosition >= 0 &&
                campusPosition < campuses.size && allRooms.isNotEmpty()) {

                val selectedCampus = campuses[campusPosition]
                val campusRooms = allRooms.filter { it.campus_id == selectedCampus.campus_id }

                if (classroomPosition < campusRooms.size) {
                    val classroomId = campusRooms[classroomPosition].classroom_id

                    lifecycleScope.launch {
                        try {
                            val response = RetrofitInstance.roomsApi.deleteClassroom(classroomId)
                            if (response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@ProfileActivity,
                                        "Аудитория успешно удалена",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                }
                            } else {
                                throw Exception("Ошибка сервера: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Ошибка: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun updateClassroomsSpinner(campusId: Int, spinner: Spinner) {
        val campusRooms = allRooms
            .filter { it.campus_id == campusId }
            .map { "Аудитория ${it.classroom_number}" }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            campusRooms
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }


    private fun showTechSetupDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_tech_setup)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val campusSpinner = dialog.findViewById<Spinner>(R.id.campusSpinner)
        val roomSpinner = dialog.findViewById<Spinner>(R.id.roomSpinner)
        val confirmBtn = dialog.findViewById<MaterialButton>(R.id.confirmBtn)

        // Показываем прогресс-бар или сообщение о загрузке
        Toast.makeText(this, "Загрузка данных...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d("ProfileActivity", "Starting API requests for tech setup...")

                // Загружаем данные кампусов
                val campusResponse = RetrofitInstance.cumpusApi.getCumpusList()
                Log.d("ProfileActivity", "Campus response code: ${campusResponse.code()}")

                if (!campusResponse.isSuccessful) {
                    throw Exception("Failed to load campuses: ${campusResponse.code()}")
                }

                campuses = campusResponse.body() ?: emptyList()
                Log.d("ProfileActivity", "Received ${campuses.size} campuses")

                // Загружаем данные аудиторий
                val roomsResponse = RetrofitInstance.roomsApi.getAllRooms()
                Log.d("ProfileActivity", "Rooms response code: ${roomsResponse.code()}")

                if (!roomsResponse.isSuccessful) {
                    throw Exception("Failed to load rooms: ${roomsResponse.code()}")
                }

                allRooms = roomsResponse.body() ?: emptyList()
                Log.d("ProfileActivity", "Received ${allRooms.size} rooms")

                runOnUiThread {
                    // Настраиваем спиннер кампусов
                    val campusAdapter = ArrayAdapter(
                        this@ProfileActivity,
                        android.R.layout.simple_spinner_item,
                        campuses.map { it.address ?: "Корпус ${it.campus_number}" }
                    )
                    campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    campusSpinner.adapter = campusAdapter

                    // Если есть кампусы, обновляем список аудиторий для первого кампуса
                    if (campuses.isNotEmpty()) {
                        updateRoomsSpinner(campuses[0].campus_id, roomSpinner)
                    }
                }

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading data for tech setup", e)
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки данных: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }

        // Обработчик выбора корпуса
        campusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < campuses.size) {
                    updateRoomsSpinner(campuses[position].campus_id, roomSpinner)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // Обработчик кнопки подтверждения
        confirmBtn.setOnClickListener {
            val campusPosition = campusSpinner.selectedItemPosition
            val roomPosition = roomSpinner.selectedItemPosition

            if (campusPosition >= 0 && roomPosition >= 0 &&
                campusPosition < campuses.size &&
                allRooms.isNotEmpty()) {

                val selectedCampus = campuses[campusPosition]
                val campusRooms = allRooms.filter { it.campus_id == selectedCampus.campus_id }

                if (roomPosition < campusRooms.size) {
                    val selectedRoom = campusRooms[roomPosition]

                    val intent = Intent(this@ProfileActivity, ClassActivity::class.java).apply {
                        putExtra("campus_id", selectedCampus.campus_id)
                        putExtra("campus_number", selectedCampus.campus_number)
                        putExtra("building_name", selectedCampus.address)
                        putExtra("classroom_id", selectedRoom.classroom_id)
                        putExtra("classroom_number", selectedRoom.classroom_number)
                        putExtra("is_admin", true)
                    }

                    startActivity(intent)
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка: выбранная аудитория не найдена",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@ProfileActivity,
                    "Пожалуйста, выберите корпус и аудиторию",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun updateRoomsSpinner(campusId: Int, roomSpinner: Spinner) {
        val campusRooms = allRooms
            .filter { it.campus_id == campusId }
            .map { "${it.classroom_number}" }

        val roomsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            campusRooms
        )
        roomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roomSpinner.adapter = roomsAdapter
    }
}