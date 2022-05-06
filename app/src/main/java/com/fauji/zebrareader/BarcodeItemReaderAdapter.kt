package com.fauji.zebrareader

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fauji.zebrareader.databinding.ListItemExpandableBinding
import java.util.*

class BarcodeItemReaderAdapter(private val context: Context, items: AbstractList<Items>) : RecyclerView.Adapter<BarcodeItemReaderAdapter.ViewHolder>() {
    private lateinit var binding: ListItemExpandableBinding
    private lateinit var adapterNavigator: AdapterNavigator
    private var items: java.util.AbstractList<Items> = ArrayList()

    init {
        this.items = items
    }

    fun setClickListener(itemClickListener: AdapterNavigator) {
        this.adapterNavigator = itemClickListener
    }

    inner class ViewHolder(var binding: ListItemExpandableBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        var logo: TextView
        var serialNumber: TextView
        var info: TextView

        init {
            binding.root.setOnClickListener(this)
            logo = binding.expandable.parentLayout.findViewById(R.id.logo)
            serialNumber = binding.expandable.parentLayout.findViewById(R.id.serialNumber)
            info = binding.expandable.secondLayout.findViewById(R.id.info)

        }

        override fun onClick(v: View?) {
            adapterNavigator.onItemClickListener(adapterPosition, binding.root, items[adapterPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ListItemExpandableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: Items = items[holder.adapterPosition]

        holder.info.text = product.productInfo
        holder.info.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                items[holder.adapterPosition].productInfo = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
        holder.serialNumber.text = product.serialNumber
        holder.logo.text = product.productCount.toString()

    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getList(): ArrayList<Items> {
        return items as ArrayList<Items>
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface AdapterNavigator {
        fun onItemClickListener(position: Int, view: View, item: Items)
    }

}