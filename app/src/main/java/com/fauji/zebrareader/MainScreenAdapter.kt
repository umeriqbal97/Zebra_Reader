package com.fauji.zebrareader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fauji.zebrareader.databinding.ListItemExpandableBinding
import com.fauji.zebrareader.databinding.ListItemViewBinding

class MainScreenAdapter(context: Context, items: java.util.AbstractList<DailyData>) : RecyclerView.Adapter<MainScreenAdapter.ViewHolder>() {

    private lateinit var binding: ListItemViewBinding
    private var items: java.util.AbstractList<DailyData> = ArrayList()
    private lateinit var mainScreenNavigator: MainScreenNavigator

    init {
        this.items = items
    }

    fun setClickListener(itemClickListener: MainScreenNavigator) {
        this.mainScreenNavigator = itemClickListener
    }

    inner class ViewHolder(var binding: ListItemViewBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.sheetName.text = item.sheet
        holder.binding.textView.text = item.date
        binding.root.setOnClickListener {
            mainScreenNavigator.onClickListener(holder.adapterPosition, items[holder.adapterPosition])
        }

        binding.download.setOnClickListener {
            mainScreenNavigator.downloadListener(holder.adapterPosition, items[holder.adapterPosition])
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface MainScreenNavigator {
        fun onClickListener(pos: Int, dailyData: DailyData)
        fun downloadListener(pos: Int, dailyData: DailyData)
    }
}