package com.example.techappnew

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.mask.Device
import com.example.techappnew.mask.Place
import com.example.techappnew.network.DeviceRequest
import com.example.techappnew.network.PlaceRequest
import com.example.techappnew.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ClassActivity : AppCompatActivity() {
    private val cellStates = Array(10) { Array(10) { false } }
    private lateinit var gridContainer: LinearLayout
    private var isAdmin = false
    private var isDelMode = false
    private var currentImageRes = R.drawable.personal_computer
    private var classroomId = 0
    private val places = mutableListOf<Place>()
    private var selectedRow = -1
    private var selectedCol = -1

    // Добавляем переменную для хранения текущего типа устройства
    private var currentDeviceType = "tech" // По умолчанию - техника

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        classroomId = intent.getIntExtra("classroom_id", 0)
        isAdmin = intent.getBooleanExtra("is_admin", false)
        val roomNumber = intent.getIntExtra("classroom_number", 0)
        val buildingName = intent.getStringExtra("building_name") ?: ""

        initUI(buildingName, roomNumber)
        loadPlaces()
    }

    private fun initUI(buildingName: String, roomNumber: Int) {
        gridContainer = findViewById(R.id.gridContainer)

        findViewById<MaterialButton>(R.id.techButton)?.setOnClickListener {
            currentImageRes = R.drawable.personal_computer
            currentDeviceType = "tech"
            updateAllCells()
        }

        findViewById<MaterialButton>(R.id.doorButton)?.setOnClickListener {
            currentImageRes = R.drawable.ic_door
            currentDeviceType = "door"
            updateAllCells()
        }

        val delButton = findViewById<MaterialButton>(R.id.delButton)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)

        delButton?.setOnClickListener { toggleDeleteMode(delButton, saveButton) }
        saveButton?.setOnClickListener { toggleDeleteMode(delButton, saveButton) }

        val selectedRoomUser = findViewById<LinearLayout>(R.id.selectedRoom)
        val selectedRoomAdmin = findViewById<LinearLayout>(R.id.selectedRoomAdmin)

        if (isAdmin) {
            selectedRoomAdmin?.visibility = View.VISIBLE
            selectedRoomUser?.visibility = View.GONE
            findViewById<TextView>(R.id.selectedRoomTextAdmin)?.text =
                "($buildingName) Аудитория $roomNumber"
        } else {
            selectedRoomUser?.visibility = View.VISIBLE
            selectedRoomAdmin?.visibility = View.GONE
            findViewById<TextView>(R.id.selectedRoomText)?.text =
                "($buildingName) Аудитория $roomNumber"
        }

        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
            selectedItemId = R.id.nav_home
            setOnNavigationItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.nav_home -> true
                    R.id.nav_profile -> {
                        startActivity(Intent(this@ClassActivity, ProfileActivity::class.java))
                        overridePendingTransition(0,0)
                        false
                    }
                    else -> false
                }
            }
        }
    }

    private fun toggleDeleteMode(delButton: MaterialButton, saveButton: MaterialButton) {
        isDelMode = !isDelMode
        delButton.visibility = if (isDelMode) View.VISIBLE else View.GONE
        saveButton.visibility = if (isDelMode) View.GONE else View.VISIBLE
    }

    private fun createGrid() {
        val sizeInPx = resources.getDimensionPixelSize(R.dimen.cell_size)
        val marginInPx = resources.getDimensionPixelSize(R.dimen.cell_margin)

        for (row in 0 until 10) {
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
            }

            for (col in 0 until 10) {
                val cell = LayoutInflater.from(this)
                    .inflate(R.layout.item_grid_cell, rowLayout, false).apply {
                        layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                            setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
                        }
                    }

                cell.setOnClickListener { handleCellClick(cell, row, col) }
                updateCellAppearance(cell, row, col)
                rowLayout.addView(cell)
            }
            gridContainer.addView(rowLayout)
        }
    }

    private fun updateCellAppearance(cell: View, row: Int, col: Int) {
        val cardView = cell.findViewById<MaterialCardView>(R.id.cardView)
        val imageView = cell.findViewById<ImageView>(R.id.cellImage)

        if (cellStates[row][col]) {
            val place = places.find { it.x == col && it.y == row }
            place?.let {
                lifecycleScope.launch {
                    try {
                        val response = RetrofitInstance.devicesApi.getDevicesByPlace(it.place_id)
                        if (response.isSuccessful) {
                            val devices = response.body() ?: emptyList()
                            runOnUiThread {
                                val iconRes = when {
                                    place.placeType == "door" || devices.any { it.status.equals("door", ignoreCase = true) } ->
                                        R.drawable.ic_door
                                    else ->
                                        R.drawable.personal_computer
                                }
                                imageView.setImageResource(iconRes)
                                imageView.visibility = View.VISIBLE
                                cardView.setCardBackgroundColor(Color.LTGRAY)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DEVICE_ICON", "Error loading devices", e)
                    }
                }
            }
        } else {
            imageView.visibility = View.INVISIBLE
            cardView.setCardBackgroundColor(Color.WHITE)
        }
    }

    private fun updateAllCells() {
        for (row in 0 until 10) {
            for (col in 0 until 10) {
                val rowLayout = gridContainer.getChildAt(row) as? LinearLayout
                rowLayout?.getChildAt(col)?.let { cell ->
                    updateCellAppearance(cell, row, col)
                }
            }
        }
    }

    private fun loadPlaces() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.placesApi.getPlaces(classroomId)
                if (response.isSuccessful) {
                    response.body()?.let { placesList ->
                        places.clear()
                        places.addAll(placesList)

                        places.forEach { place ->
                            if (place.x in 0..9 && place.y in 0..9) {
                                cellStates[place.y][place.x] = true
                            }
                        }
                        createGrid()
                    }
                } else {
                    showError("Ошибка загрузки мест: ${response.code()}")
                    createGrid()
                }
            } catch (e: Exception) {
                showError("Ошибка загрузки мест: ${e.message}")
                createGrid()
            }
        }
    }

    private fun handleCellClick(cell: View, row: Int, col: Int) {
        selectedRow = row
        selectedCol = col

        if (isAdmin) {
            if (isDelMode) {
                if (cellStates[row][col]) {
                    deletePlaceAtPosition(row, col)
                } else {
                    showToast("Место уже пустое")
                }
            } else {
                if (cellStates[row][col]) {
                    showAddDeviceDialog()
                } else {
                    addPlaceAtPosition(row, col)
                }
            }
        } else {
            showPlaceInfo(row, col)
            findViewById<LinearLayout>(R.id.selectedRoom)?.visibility = View.VISIBLE

            val place = places.find { it.x == col && it.y == row }
            if (place != null) {
                loadDevicesForPlace(place.place_id)
            } else {
                findViewById<LinearLayout>(R.id.devicesInnerContainer)?.removeAllViews()
            }
        }
    }

    private fun loadDevicesForPlace(placeId: Int) {
        if (placeId <= 0) {
            showError("Неверный ID места")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("API_REQUEST", "Запрос устройств для места ID: $placeId")
                val response = RetrofitInstance.devicesApi.getDevicesByPlace(placeId)

                if (response.isSuccessful) {
                    val devices = response.body() ?: emptyList()
                    Log.d("API_RESPONSE", "Получено устройств: ${devices.size}")
                    updateDeviceListUI(devices)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("API_ERROR", "Ошибка ${response.code()}: $errorMsg")
                    showError("Ошибка загрузки устройств: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NETWORK_ERROR", "Ошибка сети", e)
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun updateDeviceListUI(devices: List<Device>) {
        runOnUiThread {
            val scrollContainer = findViewById<ScrollView>(R.id.devicesScrollContainer)
            val devicesContainer = findViewById<LinearLayout>(R.id.devicesInnerContainer)

            devicesContainer.removeAllViews()

            if (devices.isEmpty()) {
                devicesContainer.addView(TextView(this@ClassActivity).apply {
                    text = "Устройства не найдены"
                    setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, resources.getDimensionPixelSize(R.dimen.small_margin), 0, 0)
                    }
                })
            } else {
                devices.forEach { device ->
                    val deviceView = MaterialButton(this@ClassActivity, null,
                        com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton).apply {
                        text = device.description ?: "Устройство ${device.device_id}"
                        setTextColor(ContextCompat.getColor(context, R.color.primary_color))
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                        strokeColor = ContextCompat.getColorStateList(context, R.color.primary_color)
                        strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            resources.getDimensionPixelSize(R.dimen.button_height)
                        ).apply {
                            setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.small_margin))
                        }
                        setOnClickListener { showDeviceDetailsDialog(device) }
                    }
                    devicesContainer.addView(deviceView)
                }
            }

            scrollContainer.post {
                scrollContainer.scrollTo(0, 0)
                scrollContainer.isVerticalScrollBarEnabled = true
                scrollContainer.requestLayout()
            }
        }
    }

    private fun showDeviceDetailsDialog(device: Device) {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_device_details)
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            findViewById<TextView>(R.id.deviceIdText)?.text = "ID: ${device.device_id}"
            findViewById<TextView>(R.id.placeIdText)?.text = "Место ID: ${device.place_id}"
            findViewById<TextView>(R.id.statusText)?.text = "Статус: ${device.status}"
            findViewById<TextView>(R.id.descriptionText)?.text = "Описание: ${device.description ?: "Нет описания"}"

            findViewById<MaterialButton>(R.id.viewProblemsButton)?.setOnClickListener {
                val intent = Intent(this@ClassActivity, DeviceProblemsActivity::class.java).apply {
                    putExtra("device_id", device.device_id)
                }
                startActivity(intent)
                dismiss()
            }

            findViewById<MaterialButton>(R.id.closeButton)?.setOnClickListener {
                dismiss()
            }
        }
        dialog.show()
    }

    private fun showAddDeviceDialog() {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_add_device)
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            findViewById<MaterialButton>(R.id.createDeviceBtn).setOnClickListener {
                val deviceName = findViewById<EditText>(R.id.deviceNameInput).text.toString()
                if (deviceName.isNotEmpty()) {
                    createDevice(selectedRow, selectedCol, deviceName, currentDeviceType)
                    dismiss()
                } else {
                    showToast("Введите название устройства")
                }
            }
        }
        dialog.show()
    }

    private fun createDevice(row: Int, col: Int, deviceName: String, deviceType: String) {
        lifecycleScope.launch {
            try {
                val place = places.find { it.x == col && it.y == row } ?: return@launch
                val response = RetrofitInstance.devicesApi.createDevice(
                    DeviceRequest(
                        place_id = place.place_id,
                        description = deviceName,
                        status = deviceType
                    )
                )

                if (response.isSuccessful) {
                    showToast("Устройство добавлено")
                    updateCellAtPosition(row, col)
                } else {
                    showError("Ошибка: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Ошибка: ${e.message}")
            }
        }
    }

    private fun addPlaceAtPosition(row: Int, col: Int) {
        if (places.any { it.x == col && it.y == row }) {
            Log.w("CELL_ERROR", "Место ($row, $col) уже занято")
            showToast("Место уже занято")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("API_REQUEST", "Добавление места ($row, $col) в аудиторию $classroomId")
                val response = RetrofitInstance.placesApi.createPlace(
                    PlaceRequest(
                        x = col,
                        y = row,
                        placeType = if (currentDeviceType == "door") "door" else "standard",
                        classroom_id = classroomId
                    )
                )

                if (response.isSuccessful) {
                    Log.d("API_SUCCESS", "Место добавлено: ${response.body()}")
                    response.body()?.let { place ->
                        if (place.x in 0..9 && place.y in 0..9) {
                            places.add(place)
                            cellStates[row][col] = true
                            // Создаем устройство соответствующего типа
                            createDevice(
                                row,
                                col,
                                if (currentDeviceType == "door") "Дверь" else "Компьютер",
                                currentDeviceType
                            )
                            updateCellAtPosition(row, col)
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Ошибка ${response.code()}: ${response.errorBody()?.string()}")
                    showError("Ошибка ${response.code()} при добавлении")
                }
            } catch (e: Exception) {
                Log.e("NETWORK_ERROR", "Ошибка сети", e)
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun deletePlaceAtPosition(row: Int, col: Int) {
        val place = places.find { it.x == col && it.y == row } ?: run {
            Log.w("CELL_ERROR", "Место ($row, $col) не найдено")
            showToast("Место не найдено")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("API_REQUEST", "Удаление места ID ${place.place_id}")
                val response = RetrofitInstance.placesApi.deletePlace(place.place_id)

                if (response.isSuccessful) {
                    Log.d("API_SUCCESS", "Место ${place.place_id} удалено")
                    places.remove(place)
                    cellStates[row][col] = false
                    updateCellAtPosition(row, col)
                } else {
                    Log.e("API_ERROR", "Ошибка ${response.code()}: ${response.errorBody()?.string()}")
                    showError("Ошибка ${response.code()} при удалении")
                }
            } catch (e: Exception) {
                Log.e("NETWORK_ERROR", "Ошибка сети", e)
                showError("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun showPlaceInfo(row: Int, col: Int) {
        places.find { it.x == col && it.y == row }?.let { place ->
            findViewById<TextView>(R.id.selectedTechText)?.text = "Ряд: $row, Колонка: $col"
            findViewById<TextView>(R.id.countErrorText)?.text = "ID места: ${place.place_id}"
            findViewById<LinearLayout>(R.id.selectedRoom)?.visibility = View.VISIBLE
        } ?: run {
            findViewById<TextView>(R.id.selectedTechText)?.text = "Ряд: $row, Колонка: $col"
            findViewById<TextView>(R.id.countErrorText)?.text = "Место пустое"
            findViewById<LinearLayout>(R.id.selectedRoom)?.visibility = View.GONE
        }
    }

    private fun updateCellAtPosition(row: Int, col: Int) {
        val rowLayout = gridContainer.getChildAt(row) as? LinearLayout
        rowLayout?.getChildAt(col)?.let { cell ->
            updateCellAppearance(cell, row, col)
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this@ClassActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@ClassActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}