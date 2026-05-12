package com.campus.lostfound.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.campus.lostfound.R
import com.campus.lostfound.constant.Constants
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.model.Item
import com.campus.lostfound.view.activity.DetailActivity
import com.campus.lostfound.view.adapter.ItemAdapter

class ItemListFragment : Fragment() {

    private var itemType: String? = null
    private var searchQuery: String = ""
    private lateinit var itemDao: ItemDao
    private lateinit var adapter: ItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmpty: TextView
    private lateinit var spinnerCategory: Spinner
    private var currentCategory: String? = null

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_SEARCH = "search"

        fun newInstance(type: String?, search: String = ""): ItemListFragment {
            val fragment = ItemListFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            args.putString(ARG_SEARCH, search)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemType = arguments?.getString(ARG_TYPE)
        searchQuery = arguments?.getString(ARG_SEARCH) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemDao = ItemDao(requireContext())

        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val categoryList = mutableListOf(getString(R.string.all_categories))
        categoryList.addAll(Constants.CATEGORIES)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        spinnerCategory.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                currentCategory = if (position == 0) null else categoryList[position]
                loadData()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        adapter = ItemAdapter { item ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setColorSchemeResources(com.google.android.material.R.color.design_default_color_primary)
        swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    fun loadData() {
        val items = itemDao.queryAll(itemType, currentCategory)
        // 如果有搜索关键词，进行过滤
        val filteredItems = if (searchQuery.isNotEmpty()) {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.addressText.contains(searchQuery, ignoreCase = true)
            }
        } else {
            items
        }
        adapter.setItems(filteredItems)
        tvEmpty.visibility = if (filteredItems.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filteredItems.isEmpty()) View.GONE else View.VISIBLE
        swipeRefresh.isRefreshing = false
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadData()
        }
    }
}