package com.example.techappnew

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class BuildingsAdapter(
    private val buildings: List<Building>,
    private val onRoomClick: (String, String, Int) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<BuildingsAdapter.BuildingViewHolder>() {

    data class Building(
        val name: String,
        val rooms: List<Room>,
        val buildingId: String
    )

    data class Room(
        val classroomId: Int,
        val classroomNumber: Int,
        val campusId: Int
    ) {
        fun getFormattedNumber(): String = "N°$classroomNumber"
    }

    inner class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val buildingName: TextView = itemView.findViewById(R.id.buildingName)
        val showAll: TextView = itemView.findViewById(R.id.showAll)
        val roomsRecycler: RecyclerView = itemView.findViewById(R.id.roomsRecycler)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_building, parent, false)
        return BuildingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val building = buildings[position]

        holder.buildingName.text = building.name

        holder.roomsRecycler.layoutManager = LinearLayoutManager(
            holder.itemView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Преобразуем комнаты в отображаемый формат и берем первые 4
        val displayedRooms = building.rooms.take(4).map { it.getFormattedNumber() }

        holder.roomsRecycler.adapter = RoomsAdapter(displayedRooms) { roomName ->
            // Находим соответствующую комнату по отформатированному номеру
            val room = building.rooms.firstOrNull { it.getFormattedNumber() == roomName }
            onRoomClick(roomName, building.name, room?.classroomId ?: 0)
        }

        holder.showAll.setOnClickListener {
            val context = holder.itemView.context
            val roomNumbers = building.rooms.map { it.getFormattedNumber() }

            val intent = Intent(context, RoomsActivity::class.java).apply {
                putExtra("building_name", building.name)
                putExtra("building_id", building.buildingId)
                putStringArrayListExtra("rooms_list", ArrayList(roomNumbers))
                // Передаем список ID аудиторий для RoomsActivity
                putIntegerArrayListExtra("rooms_ids", ArrayList(building.rooms.map { it.classroomId }))
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = buildings.size
}

class RoomsAdapter(
    private val rooms: List<String>,
    private val onRoomClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomImage: ShapeableImageView = itemView.findViewById(R.id.roomImage)
        val roomNumber: TextView = itemView.findViewById(R.id.roomNumber)
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.roomNumber.text = room

        val imageRes = when {
            else -> R.drawable.kot
        }
        holder.roomImage.setImageResource(imageRes)

        holder.cardView.setOnClickListener {
            onRoomClick(room)
        }
    }

    override fun getItemCount() = rooms.size
}