package com.campus.lostfound.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.campus.lostfound.R
import com.campus.lostfound.ai.AiHelper
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.util.TimeUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 发布/编辑物品页面
 * 支持发布失物/招领信息，添加图片、选择地点、GPS定位、AI分类等功能
 */
class PublishActivity : BaseActivity() {

    companion object {
        // 位置权限请求码
        private const val LOCATION_PERMISSION_REQUEST = 1002
        // 相机权限请求码
        private const val CAMERA_PERMISSION_REQUEST = 1003
    }

    // 数据管理
    private lateinit var userManager: UserManager
    private lateinit var itemDao: ItemDao
    private lateinit var aiHelper: AiHelper
    private lateinit var locationManager: LocationManager

    // 视图组件
    private lateinit var chipGroupType: ChipGroup
    private lateinit var chipLost: Chip
    private lateinit var chipFound: Chip
    private lateinit var etName: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var etLocation: TextInputEditText
    private lateinit var tvAddressInfo: TextView
    private lateinit var etTime: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnSubmit: MaterialButton

    // 状态变量
    private var imagePath: String = ""
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedAddressText: String = ""
    private var editItemId: Long = -1
    private var isLocating = false

    // 相机拍照临时URI
    private var cameraImageUri: Uri? = null

    // 相机拍照Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                val savedPath = saveImageFromUri(uri)
                if (savedPath != null) {
                    imagePath = savedPath
                    showPreview(imagePath)
                }
            }
        }
    }

    // 相册选择Launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val savedPath = saveImageFromUri(uri)
                if (savedPath != null) {
                    imagePath = savedPath
                    showPreview(imagePath)
                }
            }
        }
    }

    // 地图选点Launcher
    private val mapPointLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                selectedAddressText = data.getStringExtra("address_text") ?: ""
                etLocation.setText(selectedAddressText)
                tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
                tvAddressInfo.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish)

        // 初始化数据管理对象
        userManager = UserManager(this)
        itemDao = ItemDao(this)
        aiHelper = AiHelper()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // 检查登录状态
        if (!userManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化页面
        initViews()
        setupCategoryDropdown()
        setupListeners()
        checkEditMode()
    }

    /**
     * 初始化视图组件
     */
    private fun initViews() {
        chipGroupType = findViewById(R.id.chipGroupType)
        chipLost = findViewById(R.id.chipLost)
        chipFound = findViewById(R.id.chipFound)
        etName = findViewById(R.id.etName)
        actvCategory = findViewById(R.id.actvCategory)
        etLocation = findViewById(R.id.etLocation)
        tvAddressInfo = findViewById(R.id.tvAddressInfo)
        etTime = findViewById(R.id.etTime)
        etContact = findViewById(R.id.etContact)
        etDescription = findViewById(R.id.etDescription)
        ivPreview = findViewById(R.id.ivPreview)
        btnSubmit = findViewById(R.id.btnSubmit)

        // 默认选择"失物"类型
        chipLost.isChecked = true
    }

    /**
     * 设置分类下拉框
     */
    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES)
        actvCategory.setAdapter(adapter)
    }

    /**
     * 设置所有按钮和控件的点击事件
     */
    private fun setupListeners() {
        // 拍照按钮
        findViewById<MaterialButton>(R.id.btnCamera).setOnClickListener {
            openCamera()
        }

        // 相册选择按钮
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener {
            openGallery()
        }

        // 地图选点按钮
        findViewById<MaterialButton>(R.id.btnMapPick).setOnClickListener {
            val intent = Intent(this, MapPointActivity::class.java)
            mapPointLauncher.launch(intent)
        }

        // GPS定位按钮
        findViewById<MaterialButton>(R.id.btnGpsLocate).setOnClickListener {
            requestGpsLocation()
        }

        // AI分类按钮
        findViewById<MaterialButton>(R.id.btnAiClassify).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                val category = aiHelper.classify(name)
                actvCategory.setText(category, false)
                Toast.makeText(this, "AI推荐类型: $category", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请先输入物品名称", Toast.LENGTH_SHORT).show()
            }
        }

        // 时间选择点击
        etTime.setOnClickListener {
            showDatePicker()
        }

        // 提交按钮
        btnSubmit.setOnClickListener {
            submitItem()
        }
    }

    /**
     * 检查是否是编辑模式
     * 如果是编辑模式，填充原物品信息
     */
    private fun checkEditMode() {
        editItemId = intent.getLongExtra("item_id", -1)
        if (editItemId > 0) {
            val item = itemDao.queryById(editItemId) ?: return
            // 填充物品信息
            etName.setText(item.name)
            actvCategory.setText(item.category, false)
            etLocation.setText(item.location)
            etTime.setText(item.time)
            etContact.setText(item.contact)
            etDescription.setText(item.description)
            // 设置类型
            if (item.type == Constants.ITEM_TYPE_LOST) chipLost.isChecked = true else chipFound.isChecked = true
            // 显示图片
            if (item.imagePath.isNotEmpty()) {
                imagePath = item.imagePath
                showPreview(imagePath)
            }
            // 设置位置信息
            selectedLatitude = item.latitude
            selectedLongitude = item.longitude
            selectedAddressText = item.addressText
            if (selectedAddressText.isNotEmpty()) {
                tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
                tvAddressInfo.visibility = android.view.View.VISIBLE
            }
            // 修改按钮文字和标题
            btnSubmit.text = "更新"
            title = "编辑信息"
        }
    }

    /**
     * 打开相机拍照
     */
    private fun openCamera() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
            return
        }

        // 权限已授予，打开相机
        try {
            val imageFile = File(createImageDir(), "IMG_${System.currentTimeMillis()}.jpg")
            cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开相册选择图片
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    /**
     * 创建图片存储目录
     */
    private fun createImageDir(): File {
        val dir = File(filesDir, Constants.IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 从URI保存图片到本地
     * @param uri 图片URI
     * @return 保存后的文件路径
     */
    private fun saveImageFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(createImageDir(), "IMG_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 显示图片预览
     */
    private fun showPreview(path: String) {
        ivPreview.visibility = android.view.View.VISIBLE
        Glide.with(this).load(File(path)).centerCrop().into(ivPreview)
    }

    /**
     * 显示日期选择器
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etTime.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * 提交物品信息
     */
    private fun submitItem() {
        // 获取表单数据
        val type = if (chipLost.isChecked) Constants.ITEM_TYPE_LOST else Constants.ITEM_TYPE_FOUND
        val name = etName.text.toString().trim()
        val category = actvCategory.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val time = etTime.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // 验证必填项
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入物品名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (contact.isEmpty()) {
            Toast.makeText(this, "请输入联系方式", Toast.LENGTH_SHORT).show()
            return
        }

        val publisher = userManager.getCurrentUser()
        if (publisher.isEmpty()) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建物品对象
        val item = Item(
            id = if (editItemId > 0) editItemId else 0,
            type = type,
            name = name,
            category = category,
            location = location,
            time = time,
            contact = contact,
            description = description,
            imagePath = imagePath,
            publisher = publisher,
            publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp()),
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            addressText = selectedAddressText
        )

        // 插入或更新数据库
        val success = if (editItemId > 0) {
            itemDao.update(item) > 0
        } else {
            itemDao.insert(item) > 0
        }

        if (success) {
            Toast.makeText(this, getString(R.string.publish_success), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, getString(R.string.publish_fail), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 请求GPS定位权限
     */
    private fun requestGpsLocation() {
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

        startGpsLocation()
    }

    /**
     * 开始GPS定位
     */
    @SuppressLint("MissingPermission")
    private fun startGpsLocation() {
        isLocating = true
        Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show()

        // 先尝试获取最后已知位置
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            onGpsLocationReceived(lastKnownLocation)
            isLocating = false
            return
        }

        // 注册位置监听器获取新位置
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                onGpsLocationReceived(location)
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
     * GPS位置获取成功回调
     */
    private fun onGpsLocationReceived(location: Location) {
        selectedLatitude = location.latitude
        selectedLongitude = location.longitude
        selectedAddressText = reverseGeocode(location.latitude, location.longitude)
        etLocation.setText(selectedAddressText)
        tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
        tvAddressInfo.visibility = android.view.View.VISIBLE
        Toast.makeText(this, "定位成功: $selectedAddressText", Toast.LENGTH_SHORT).show()
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
        
        // 处理位置权限请求
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGpsLocation()
            } else {
                Toast.makeText(this, "需要位置权限才能使用GPS定位", Toast.LENGTH_LONG).show()
            }
        }
        
        // 处理相机权限请求
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新打开相机
                openCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_LONG).show()
            }
        }
    }
}