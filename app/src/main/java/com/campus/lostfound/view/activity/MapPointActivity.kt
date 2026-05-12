package com.campus.lostfound.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campus.lostfound.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * 校园地点数据类
 */
data class CampusLocation(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * 地图选点页面
 * 用于选择物品丢失或发现的地点，支持：
 * - 从预设校园地点中选择
 * - 搜索过滤地点
 * - GPS定位获取当前位置
 * - 手动输入经纬度
 */
class MapPointActivity : BaseActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    // 预设校园地点列表
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

    // 过滤后的地点列表
    private var filteredLocations = campusLocations
    // 选中的位置信息
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedAddress: String = ""

    private lateinit var locationManager: LocationManager
    private lateinit var adapter: LocationAdapter
    private var isLocating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_point)
        title = "选择地点"

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // 初始化视图组件
        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerLocations)
        val tvSelectedAddress = findViewById<TextView>(R.id.tvSelectedAddress)
        val etLatitude = findViewById<TextInputEditText>(R.id.etLatitude)
        val etLongitude = findViewById<TextInputEditText>(R.id.etLongitude)
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCurrentLocation = findViewById<MaterialButton>(R.id.btnCurrentLocation)

        // 设置RecyclerView适配器
        adapter = LocationAdapter(filteredLocations) { location ->
            selectLocation(location.latitude, location.longitude, location.name, tvSelectedAddress, etLatitude, etLongitude)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 搜索框监听：实时过滤地点
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterLocations(s.toString())
            }
        })

        // GPS定位按钮
        btnCurrentLocation.setOnClickListener {
            requestCurrentLocation(tvSelectedAddress, etLatitude, etLongitude)
        }

        // 确认按钮：返回选择的地点
        btnConfirm.setOnClickListener {
            // 如果未选择，尝试从输入框获取
            if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
                val lat = etLatitude.text.toString().toDoubleOrNull() ?: 0.0
                val lng = etLongitude.text.toString().toDoubleOrNull() ?: 0.0
                if (lat != 0.0 && lng != 0.0) {
                    selectedLatitude = lat
                    selectedLongitude = lng
                    selectedAddress = tvSelectedAddress.text.toString()
                }
            }

            // 验证是否已选择地点
            if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
                Toast.makeText(this, "请选择地点或使用GPS定位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 返回结果给上一个页面
            val intent = Intent()
            intent.putExtra("latitude", selectedLatitude)
            intent.putExtra("longitude", selectedLongitude)
            intent.putExtra("address_text", selectedAddress)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    /**
     * 根据搜索词过滤地点列表
     */
    private fun filterLocations(query: String) {
        filteredLocations = if (query.isBlank()) {
            campusLocations
        } else {
            campusLocations.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.detail.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filteredLocations)
    }

    /**
     * 选择地点并更新界面
     */
    private fun selectLocation(
        lat: Double, lng: Double, address: String,
        tvSelectedAddress: TextView, etLatitude: TextInputEditText, etLongitude: TextInputEditText
    ) {
        selectedLatitude = lat
        selectedLongitude = lng
        selectedAddress = address
        tvSelectedAddress.text = "已选择: $address"
        etLatitude.setText(String.format("%.6f", lat))
        etLongitude.setText(String.format("%.6f", lng))
    }

    /**
     * 请求当前位置权限
     */
    private fun requestCurrentLocation(
        tvSelectedAddress: TextView, etLatitude: TextInputEditText, etLongitude: TextInputEditText
    ) {
        if (isLocating) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        startLocationRequest(tvSelectedAddress, etLatitude, etLongitude)
    }

    /**
     * 开始定位请求
     */
    @SuppressLint("MissingPermission")
    private fun startLocationRequest(
        tvSelectedAddress: TextView, etLatitude: TextInputEditText, etLongitude: TextInputEditText
    ) {
        isLocating = true
        Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show()

        // 先尝试获取最后已知位置
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            onLocationReceived(lastKnownLocation, tvSelectedAddress, etLatitude, etLongitude)
            isLocating = false
            return
        }

        // 注册位置监听器获取新位置
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                onLocationReceived(location, tvSelectedAddress, etLatitude, etLongitude)
                isLocating = false
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0L, 0f, locationListener
            )
        } catch (e: Exception) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener
                )
            } catch (e2: Exception) {
                isLocating = false
                Toast.makeText(this, "无法获取位置，请检查GPS是否开启", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 位置获取成功回调
     */
    private fun onLocationReceived(
        location: Location,
        tvSelectedAddress: TextView, etLatitude: TextInputEditText, etLongitude: TextInputEditText
    ) {
        val lat = location.latitude
        val lng = location.longitude
        val address = reverseGeocode(lat, lng)

        selectLocation(lat, lng, address, tvSelectedAddress, etLatitude, etLongitude)
        Toast.makeText(this, "定位成功: $address", Toast.LENGTH_SHORT).show()
    }

    /**
     * 反向地理编码：将经纬度转换为地址文字
     */
    private fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.CHINA)
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val sb = StringBuilder()
                address.thoroughfare?.let { sb.append(it) }
                address.featureName?.let {
                    if (sb.isNotEmpty()) sb.append("附近")
                }
                if (sb.isEmpty()) {
                    address.locality?.let { sb.append(it) }
                    address.subLocality?.let { sb.append(it) }
                }
                if (sb.isEmpty()) {
                    String.format("经度%.4f, 纬度%.4f", lng, lat)
                } else {
                    sb.toString()
                }
            } else {
                String.format("经度%.4f, 纬度%.4f", lng, lat)
            }
        } catch (e: Exception) {
            String.format("经度%.4f, 纬度%.4f", lng, lat)
        }
    }

    /**
     * 权限请求结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val tvSelectedAddress = findViewById<TextView>(R.id.tvSelectedAddress)
                val etLatitude = findViewById<TextInputEditText>(R.id.etLatitude)
                val etLongitude = findViewById<TextInputEditText>(R.id.etLongitude)
                startLocationRequest(tvSelectedAddress, etLatitude, etLongitude)
            } else {
                Toast.makeText(this, "需要位置权限才能使用GPS定位", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 地点列表RecyclerView适配器
     */
    inner class LocationAdapter(
        private var locations: List<CampusLocation>,
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

        fun updateData(newLocations: List<CampusLocation>) {
            locations = newLocations
            notifyDataSetChanged()
        }
    }
}