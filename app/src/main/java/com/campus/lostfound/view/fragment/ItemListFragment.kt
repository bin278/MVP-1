package com.campus.lostfound.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campus.lostfound.R
import com.campus.lostfound.db.ItemDao
import com.campus.lostfound.sharedpref.UserManager
import com.campus.lostfound.view.activity.DetailActivity
import com.campus.lostfound.view.adapter.PostAdapter

class ItemListFragment : Fragment() {

    private var itemType: String? = null
    private var searchQuery: String = ""
    private lateinit var itemDao: ItemDao
    private lateinit var userManager: UserManager
    private lateinit var adapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

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
        userManager = UserManager(requireContext())

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = PostAdapter({ item ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }, userManager)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadData()
    }

    fun loadData() {
        val items = itemDao.queryAll(itemType, null)
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
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadData()
    }
}