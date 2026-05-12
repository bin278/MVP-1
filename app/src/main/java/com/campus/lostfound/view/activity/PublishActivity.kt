package com.campus.lostfound.view.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class PublishActivity : BaseActivity() {

    private lateinit var userManager: UserManager
    private lateinit var itemDao: ItemDao
    private lateinit var aiHelper: AiHelper

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

    private var imagePath: String = ""
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedAddressText: String = ""
    private var editItemId: Long = -1

    private var cameraImageUri: Uri? = null

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

        userManager = UserManager(this)
        itemDao = ItemDao(this)
        aiHelper = AiHelper()

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
        btnSubmit = findViewById(R.id.btnSubmit)

        chipLost.isChecked = true
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES)
        actvCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnCamera).setOnClickListener {
            openCamera()
        }

        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener {
            openGallery()
        }

        findViewById<MaterialButton>(R.id.btnMapPick).setOnClickListener {
            val intent = Intent(this, MapPointActivity::class.java)
            mapPointLauncher.launch(intent)
        }

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

        etTime.setOnClickListener {
            showDatePicker()
        }

        btnSubmit.setOnClickListener {
            submitItem()
        }
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
            if (item.imagePath.isNotEmpty()) {
                imagePath = item.imagePath
                showPreview(imagePath)
            }
            selectedLatitude = item.latitude
            selectedLongitude = item.longitude
            selectedAddressText = item.addressText
            if (selectedAddressText.isNotEmpty()) {
                tvAddressInfo.text = "经度: $selectedLongitude, 纬度: $selectedLatitude"
                tvAddressInfo.visibility = android.view.View.VISIBLE
            }
            btnSubmit.text = "更新"
            title = "编辑信息"
        }
    }

    private fun openCamera() {
        val imageFile = File(createImageDir(), "IMG_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageDir(): File {
        val dir = File(filesDir, Constants.IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

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

    private fun showPreview(path: String) {
        ivPreview.visibility = android.view.View.VISIBLE
        Glide.with(this).load(File(path)).centerCrop().into(ivPreview)
    }

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

    private fun submitItem() {
        val type = if (chipLost.isChecked) Constants.ITEM_TYPE_LOST else Constants.ITEM_TYPE_FOUND
        val name = etName.text.toString().trim()
        val category = actvCategory.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val time = etTime.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val description = etDescription.text.toString().trim()

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
}
