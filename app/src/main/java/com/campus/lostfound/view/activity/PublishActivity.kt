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
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
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
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

/**
 * 发布/编辑物品页面
 * 支持发布失物/招领信息，上传多张图片、选择地点、GPS定位、AI分类等
 */
class PublishActivity : BaseActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1002
        private const val CAMERA_PERMISSION_REQUEST = 1003
        private const val IMAGE_SEPARATOR = "|||"
        private const val MAX_IMAGE_COUNT = 9
    }

    private lateinit var userManager: UserManager
    private lateinit var itemDao: ItemDao
    private lateinit var aiHelper: AiHelper
    private lateinit var locationManager: LocationManager

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
    private lateinit var hsvImages: android.widget.HorizontalScrollView
    private lateinit var layoutImageList: LinearLayout
    private lateinit var btnSubmit: MaterialButton

    private val imagePaths = mutableListOf<String>()
    private var selectedLatitude = 0.0
    private var selectedLongitude = 0.0
    private var selectedAddressText = ""
    private var editItemId = -1L
    private var isLocating = false

    private var cameraImageUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                addImageFromUri(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> addImageFromUri(uri) }
        }
    }

    private val multiGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> addImageFromUri(uri) }
    }

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
                tvAddressInfo.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        userManager = UserManager(this)
        itemDao = ItemDao(this)
        aiHelper = AiHelper()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!userManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupCategoryDropdown()
        setupListeners()
        checkEditMode()
    }

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
        hsvImages = findViewById(R.id.hsvImages)
        layoutImageList = findViewById(R.id.layoutImageList)
        btnSubmit = findViewById(R.id.btnSubmit)

        chipLost.isChecked = true
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES)
        actvCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnCamera).setOnClickListener { openCamera() }
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener { openGallery() }

        findViewById<MaterialButton>(R.id.btnMapPick).setOnClickListener {
            mapPointLauncher.launch(Intent(this, MapPointActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnGpsLocate).setOnClickListener { requestGpsLocation() }

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

        etTime.setOnClickListener { showDatePicker() }
        btnSubmit.setOnClickListener { submitItem() }
    }

    private fun checkEditMode() {
        editItemId = intent.getLongExtra("item_id", -1)
        if (editItemId > 0) {
            val item = itemDao.queryById(editItemId) ?: return
            etName.setText(item.name)
            actvCategory.setText(item.category, false)
            etLocation.setText(item.location)
            etTime.setText(item.time)
            etContact.setText(item.contact)
            etDescription.setText(item.description)
            if (item.type == Constants.ITEM_TYPE_LOST) chipLost.isChecked = true else chipFound.isChecked = true
            selectedLatitude = item.latitude
            selectedLongitude = item.longitude
            selectedAddressText = item.addressText
            if (selectedAddressText.isNotEmpty()) {
                tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
                tvAddressInfo.visibility = View.VISIBLE
            }
            // 解析已有图片路径
            if (item.imagePath.isNotEmpty()) {
                item.imagePath.split(IMAGE_SEPARATOR).forEach { path ->
                    if (path.isNotEmpty() && File(path).exists()) {
                        imagePaths.add(path)
                    }
                }
                refreshImagePreviews()
            }
            btnSubmit.text = "更新"
            title = "编辑信息"
        }
    }

    private fun openCamera() {
        if (imagePaths.size >= MAX_IMAGE_COUNT) {
            Toast.makeText(this, "最多上传${MAX_IMAGE_COUNT}张图片", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            return
        }
        try {
            val imageFile = File(createImageDir(), "IMG_${System.currentTimeMillis()}.jpg")
            cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        if (imagePaths.size >= MAX_IMAGE_COUNT) {
            Toast.makeText(this, "最多上传${MAX_IMAGE_COUNT}张图片", Toast.LENGTH_SHORT).show()
            return
        }
        multiGalleryLauncher.launch("image/*")
    }

    private fun createImageDir(): File {
        val dir = File(filesDir, Constants.IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun addImageFromUri(uri: Uri) {
        if (imagePaths.size >= MAX_IMAGE_COUNT) {
            Toast.makeText(this, "最多上传${MAX_IMAGE_COUNT}张图片", Toast.LENGTH_SHORT).show()
            return
        }
        val savedPath = saveImageFromUri(uri)
        if (savedPath != null) {
            imagePaths.add(savedPath)
            refreshImagePreviews()
        }
    }

    private fun saveImageFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(createImageDir(), "IMG_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output -> inputStream.copyTo(output) }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshImagePreviews() {
        // 隐藏单图预览
        ivPreview.visibility = View.GONE
        layoutImageList.removeAllViews()

        if (imagePaths.isEmpty()) {
            hsvImages.visibility = View.GONE
            return
        }

        hsvImages.visibility = View.VISIBLE
        val displayMetrics = resources.displayMetrics
        val imgSize = (110 * displayMetrics.density).toInt()

        imagePaths.forEachIndexed { index, path ->
            // 图片容器
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 8.dp, 0)
            }

            val iv = ShapeableImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(imgSize, imgSize)
                scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this@PublishActivity)
                    .load(File(path))
                    .transform(RoundedCorners(8.dp))
                    .centerCrop()
                    .into(this)
            }

            // 删除按钮
            val tvDel = TextView(this).apply {
                text = "✕"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@PublishActivity, R.color.red))
                gravity = Gravity.CENTER
                setPadding(0, 4.dp, 0, 0)
                setOnClickListener {
                    imagePaths.removeAt(index)
                    refreshImagePreviews()
                }
            }

            container.addView(iv)
            container.addView(tvDel)
            layoutImageList.addView(container)
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                etTime.setText(String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitItem() {
        val type = if (chipLost.isChecked) Constants.ITEM_TYPE_LOST else Constants.ITEM_TYPE_FOUND
        val name = etName.text.toString().trim()
        val category = actvCategory.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val time = etTime.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (name.isEmpty()) { Toast.makeText(this, "请输入物品名称", Toast.LENGTH_SHORT).show(); return }
        if (contact.isEmpty()) { Toast.makeText(this, "请输入联系方式", Toast.LENGTH_SHORT).show(); return }

        val publisher = userManager.getCurrentUser()
        if (publisher.isEmpty()) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        val joinedPath = imagePaths.joinToString(IMAGE_SEPARATOR)

        val item = Item(
            id = if (editItemId > 0) editItemId else 0,
            type = type, name = name, category = category,
            location = location, time = time, contact = contact,
            description = description, imagePath = joinedPath,
            publisher = publisher,
            publishTime = TimeUtil.formatTimestamp(TimeUtil.currentTimestamp()),
            latitude = selectedLatitude, longitude = selectedLongitude,
            addressText = selectedAddressText
        )

        val success = if (editItemId > 0) itemDao.update(item) > 0 else itemDao.insert(item) > 0
        if (success) {
            Toast.makeText(this, getString(R.string.publish_success), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, getString(R.string.publish_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestGpsLocation() {
        if (isLocating) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
            return
        }
        startGpsLocation()
    }

    @SuppressLint("MissingPermission")
    private fun startGpsLocation() {
        isLocating = true
        Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show()
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (lastKnownLocation != null) { onGpsLocationReceived(lastKnownLocation); isLocating = false; return }
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) { locationManager.removeUpdates(this); onGpsLocationReceived(loc); isLocating = false }
            @Deprecated("Deprecated") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
        } catch (e: Exception) {
            try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) }
            catch (e2: Exception) { isLocating = false; Toast.makeText(this, "无法获取位置", Toast.LENGTH_LONG).show() }
        }
    }

    private fun onGpsLocationReceived(location: Location) {
        selectedLatitude = location.latitude; selectedLongitude = location.longitude
        selectedAddressText = reverseGeocode(location.latitude, location.longitude)
        etLocation.setText(selectedAddressText)
        tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
        tvAddressInfo.visibility = View.VISIBLE
        Toast.makeText(this, "定位成功: $selectedAddressText", Toast.LENGTH_SHORT).show()
    }

    private fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.CHINA)
            val addresses = geocoder.getFromLocation(lat, lng, 1) ?: emptyList()
            if (addresses.isNotEmpty()) {
                val sb = StringBuilder()
                addresses[0].thoroughfare?.let { sb.append(it) }
                addresses[0].featureName?.let { if (sb.isNotEmpty()) sb.append("附近") }
                if (sb.isEmpty()) { addresses[0].locality?.let { sb.append(it) }; addresses[0].subLocality?.let { sb.append(it) } }
                sb.ifEmpty { "经度${"%.4f".format(lng)}, 纬度${"%.4f".format(lat)}" }.toString()
            } else "经度${"%.4f".format(lng)}, 纬度${"%.4f".format(lat)}"
        } catch (e: Exception) { "经度${"%.4f".format(lng)}, 纬度${"%.4f".format(lat)}" }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGpsLocation()
        }
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }
}
