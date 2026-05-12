package com.campus.lostfound.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campus.lostfound.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

data class CampusLocation(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double
)

class MapPointActivity : BaseActivity() {

    private val campusLocations = listOf(
        CampusLocation("图书馆", "主校区图书馆", 30.755, 103.935),
        CampusLocation("第一教学楼", "主校区教学楼A栋", 30.756, 103.936),
        CampusLocation("第二教学楼", "主校区教学楼B栋", 30.757, 103.937),
        CampusLocation("学生食堂", "主校区第一食堂", 30.758, 103.934),
        CampusLocation("体育馆", "主校区综合体育馆", 30.759, 103.938),
        CampusLocation("学生宿舍1栋", "主校区学生宿舍区", 30.760, 103.933),
        CampusLocation("学生宿舍2栋", "主校区学生宿舍区", 30.761, 103.934),
        CampusLocation("行政楼", "主校区行政办公区", 30.754, 103.936),
        CampusLocation("实验楼", "主校区实验中心", 30.756, 103.939),
        CampusLocation("操场", "主校区田径场", 30.758, 103.940),
        CampusLocation("校医院", "主校区医疗中心", 30.753, 103.935),
        CampusLocation("西门", "主校区西大门", 30.755, 103.931),
        CampusLocation("东门", "主校区东大门", 30.755, 103.942),
        CampusLocation("南门", "主校区南大门", 30.751, 103.936),
        CampusLocation("北门", "主校区北大门", 30.763, 103.936)
    )

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_point)
        title = "选择地点"

        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerLocations)
        val tvSelectedAddress = findViewById<TextView>(R.id.tvSelectedAddress)
        val etLatitude = findViewById<TextInputEditText>(R.id.etLatitude)
        val etLongitude = findViewById<TextInputEditText>(R.id.etLongitude)
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirm)

        val adapter = LocationAdapter(campusLocations) { location ->
            selectedLatitude = location.latitude
            selectedLongitude = location.longitude
            selectedAddress = location.name
            tvSelectedAddress.text = location.name
            etLatitude.setText(location.latitude.toString())
            etLongitude.setText(location.longitude.toString())
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnConfirm.setOnClickListener {
            if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
                val lat = etLatitude.text.toString().toDoubleOrNull() ?: 0.0
                val lng = etLongitude.text.toString().toDoubleOrNull() ?: 0.0
                if (lat != 0.0 && lng != 0.0) {
                    selectedLatitude = lat
                    selectedLongitude = lng
                    selectedAddress = tvSelectedAddress.text.toString()
                }
            }

            val intent = Intent()
            intent.putExtra("latitude", selectedLatitude)
            intent.putExtra("longitude", selectedLongitude)
            intent.putExtra("address_text", selectedAddress)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    inner class LocationAdapter(
        private val locations: List<CampusLocation>,
        private val onClick: (CampusLocation) -> Unit
    ) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvLocationName)
            val tvDetail: TextView = view.findViewById(R.id.tvLocationDetail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val location = locations[position]
            holder.tvName.text = location.name
            holder.tvDetail.text = location.detail
            holder.itemView.setOnClickListener { onClick(location) }
        }

        override fun getItemCount() = locations.size
    }
}
