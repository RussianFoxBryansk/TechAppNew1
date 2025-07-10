package com.example.techappnew

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.techappnew.mask.Cumpus
import com.example.techappnew.mask.Device
import com.example.techappnew.mask.Place
import com.example.techappnew.network.ClassroomRequest
import com.example.techappnew.network.DevicesApi
import com.example.techappnew.network.PlacesApi
import com.example.techappnew.network.ProblemRequest
import com.example.techappnew.network.RoomsApi
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProblemDialog(
    private val context: Context,
    private val userId: Int,
    private val campuses: List<Cumpus>,
    private val roomsApi: RoomsApi,
    private val placesApi: PlacesApi,
    private val devicesApi: DevicesApi,
    private val onProblemCreated: (ProblemRequest) -> Unit,
    override val lifecycle: Lifecycle
) : Dialog(context), LifecycleOwner {

    private lateinit var campusSpinner: Spinner
    private lateinit var classroomSpinner: Spinner
    private lateinit var deviceSpinner: Spinner
    private lateinit var problemDescriptionInput: TextInputEditText
    private lateinit var createProblemBtn: MaterialButton
    private lateinit var hiddenUserId: TextView
    private lateinit var hiddenDeviceId: TextView
    private lateinit var placeGridContainer: LinearLayout
    private lateinit var toggleGridButton: MaterialButton
    private lateinit var gridScrollContainer: View

    private var selectedCampusId: Int? = null
    private var selectedClassroomId: Int? = null
    private var selectedPlaceId: Int? = null
    private var selectedDeviceId: Int? = null
    private val cellStates = Array(10) { Array(10) { false } }
    private var selectedPlaceRow = -1
    private var selectedPlaceCol = -1
    private var placesList: List<Place> = emptyList()
    private var isGridExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_problem)

        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        initViews()
        setupCampusSpinner()
        setupListeners()

        hiddenUserId.text = userId.toString()
    }

    private fun initViews() {
        campusSpinner = findViewById(R.id.campusSpinner)
        classroomSpinner = findViewById(R.id.classroomSpinner)
        placeGridContainer = findViewById(R.id.placeGridContainer)
        deviceSpinner = findViewById(R.id.deviceSpinner)
        problemDescriptionInput = findViewById(R.id.problemDescriptionInput)
        createProblemBtn = findViewById(R.id.createProblemBtn)
        hiddenUserId = findViewById(R.id.hiddenUserId)
        hiddenDeviceId = findViewById(R.id.hiddenDeviceId)
        toggleGridButton = findViewById(R.id.toggleGridButton)
        gridScrollContainer = findViewById(R.id.placeGridScroll)
    }

    private fun setupListeners() {
        createProblemBtn.setOnClickListener {
            val description = problemDescriptionInput.text.toString()

            if (validateInputs(description)) {
                val problemRequest = ProblemRequest(
                    device_id = selectedDeviceId ?: 0,
                    user_id = userId,
                    description = description,
                    active = true,
                    status = "Pending"
                )

                onProblemCreated(problemRequest)
                dismiss()
            } else {
                showError("Заполните все поля")
            }
        }

        toggleGridButton.setOnClickListener {
            isGridExpanded = !isGridExpanded
            updateGridVisibility()
        }
    }

    private fun updateGridVisibility() {
        if (isGridExpanded) {
            gridScrollContainer.visibility = View.VISIBLE
            toggleGridButton.text = "Свернуть сетку"
            toggleGridButton.setIconResource(R.drawable.ic_expand_less)
        } else {
            gridScrollContainer.visibility = View.GONE
            toggleGridButton.text = "Развернуть сетку"
            toggleGridButton.setIconResource(R.drawable.ic_expand_more)
        }
    }

    private fun setupCampusSpinner() {
        val campusAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            campuses.map { it.address ?: "Корпус ${it.campus_number}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        campusSpinner.adapter = campusAdapter

        campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCampusId = campuses[position].campus_id
                loadClassrooms(campuses[position].campus_id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadClassrooms(campusId: Int) {
        getClassroomsForCampus(
            campusId = campusId,
            onSuccess = { classrooms ->
                setupClassroomSpinner(classrooms)
            },
            onError = { e ->
                showError("Ошибка загрузки аудиторий: ${e.message}")
            }
        )
    }

    private fun setupClassroomSpinner(classrooms: List<ClassroomRequest>) {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            classrooms.map { "Аудитория ${it.classroom_number}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        classroomSpinner.adapter = adapter

        classroomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedClassroomId = classrooms[position].classroom_id
                loadPlaces(classrooms[position].classroom_id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadPlaces(classroomId: Int) {
        getPlacesForClassroom(
            classroomId = classroomId,
            onSuccess = { places ->
                placesList = places
                createPlaceGrid(places)
            },
            onError = { e ->
                showError("Ошибка загрузки клеток: ${e.message}")
            }
        )
    }

    private fun createPlaceGrid(places: List<Place>) {
        placeGridContainer.removeAllViews()

        // Сброс состояний
        for (row in 0 until 10) {
            for (col in 0 until 10) {
                cellStates[row][col] = false
            }
        }

        // Рассчитываем размер клетки динамически
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels -
                context.resources.getDimensionPixelSize(R.dimen.dialog_padding) * 2
        val cellSize = screenWidth / 10 // 10 клеток в строке

        // Помечаем занятые места
        places.forEach { place ->
            if (place.x in 0..9 && place.y in 0..9) {
                cellStates[place.y][place.x] = true
            }
        }

        for (row in 0 until 10) {
            val rowLayout = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
            }

            for (col in 0 until 10) {
                val cell = LayoutInflater.from(context)
                    .inflate(R.layout.item_grid_cell, rowLayout, false).apply {
                        layoutParams = LinearLayout.LayoutParams(cellSize, cellSize)
                    }

                cell.setOnClickListener {
                    handlePlaceCellClick(cell, row, col)
                }
                updatePlaceCellAppearance(cell, row, col)
                rowLayout.addView(cell)
            }
            placeGridContainer.addView(rowLayout)
        }
    }

    private fun handlePlaceCellClick(cell: View, row: Int, col: Int) {
        if (!cellStates[row][col]) {
            showError("Эта клетка пустая")
            return
        }

        selectedPlaceRow = row
        selectedPlaceCol = col

        // Сброс предыдущего выбора
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                val rowLayout = placeGridContainer.getChildAt(r) as? LinearLayout
                rowLayout?.getChildAt(c)?.let { updatePlaceCellAppearance(it, r, c) }
            }
        }

        // Подсветка выбранной клетки
        updatePlaceCellAppearance(cell, row, col)

        // Находим соответствующее место и загружаем устройства
        val place = placesList.find { it.x == col && it.y == row }
        place?.let {
            selectedPlaceId = it.place_id
            loadDevices(it.place_id)
        }
    }

    private fun updatePlaceCellAppearance(cell: View, row: Int, col: Int) {
        val cardView = cell.findViewById<MaterialCardView>(R.id.cardView)
        val imageView = cell.findViewById<ImageView>(R.id.cellImage)

        // Устанавливаем красный фон для выбранной клетки, серый для занятых, белый для пустых
        when {
            row == selectedPlaceRow && col == selectedPlaceCol -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_red))
                imageView.setImageResource(R.drawable.personal_computer)
                imageView.visibility = View.VISIBLE
            }
            cellStates[row][col] -> {
                cardView.setCardBackgroundColor(Color.LTGRAY)
                imageView.setImageResource(R.drawable.personal_computer)
                imageView.visibility = View.VISIBLE
            }
            else -> {
                cardView.setCardBackgroundColor(Color.WHITE)
                imageView.visibility = View.INVISIBLE
            }
        }
    }

    private fun loadDevices(placeId: Int) {
        getDevicesForPlace(
            placeId = placeId,
            onSuccess = { devices ->
                setupDeviceSpinner(devices)
            },
            onError = { e ->
                showError("Ошибка загрузки устройств: ${e.message}")
            }
        )
    }

    private fun setupDeviceSpinner(devices: List<Device>) {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            devices.map { it.description ?: "Устройство ${it.device_id}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        deviceSpinner.adapter = adapter

        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDeviceId = devices[position].device_id
                hiddenDeviceId.text = devices[position].device_id.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun validateInputs(description: String): Boolean {
        return selectedDeviceId != null &&
                selectedPlaceRow != -1 &&
                selectedPlaceCol != -1 &&
                description.isNotBlank()
    }

    private fun getClassroomsForCampus(
        campusId: Int,
        onSuccess: (List<ClassroomRequest>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = roomsApi.getAllRooms()
                if (response.isSuccessful) {
                    val classrooms = response.body()?.filter {
                        it.campus_id == campusId
                    } ?: emptyList()
                    onSuccess(classrooms)
                } else {
                    onError(Exception("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun getPlacesForClassroom(
        classroomId: Int,
        onSuccess: (List<Place>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = placesApi.getPlaces(classroomId)
                if (response.isSuccessful) {
                    onSuccess(response.body() ?: emptyList())
                } else {
                    onError(Exception("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun getDevicesForPlace(
        placeId: Int,
        onSuccess: (List<Device>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = devicesApi.getDevicesByPlace(placeId)
                if (response.isSuccessful) {
                    onSuccess(response.body() ?: emptyList())
                } else {
                    onError(Exception("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}