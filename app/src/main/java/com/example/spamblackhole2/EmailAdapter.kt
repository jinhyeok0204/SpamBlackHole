package com.example.spamblackhole2

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spamblackhole2.databinding.EmailItemBinding
import com.example.spamblackhole2.databinding.LoadingItemBinding

class EmailAdapter(private var emailList: List<EmailData>, private val onEmailClicked: (EmailData) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private var isLoadingAdded = false


    inner class EmailViewHolder(val binding: EmailItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(email:EmailData){
            binding.emailSubject.text = email.subject
            binding.emailSnippet.text = email.snippet
            itemView.setOnClickListener{
                onEmailClicked(email)
            }
        }
    }

    inner class LoadingViewHolder(val binding: LoadingItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == VIEW_TYPE_ITEM){
            val binding = EmailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            EmailViewHolder(binding)
        } else{
            val binding = LoadingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            LoadingViewHolder(binding)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is EmailViewHolder){
            holder.bind(emailList[position])
        }
    }

    override fun getItemCount(): Int {
        return emailList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == emailList.size - 1 && isLoadingAdded) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    fun addLoadingFooter(){
        isLoadingAdded = true
        emailList = emailList + EmailData("", "Loading", "")
        notifyItemInserted(emailList.size - 1)
    }

    fun removeLoadingFooter(){
        if(isLoadingAdded){
            isLoadingAdded = false
            val position = emailList.size - 1
            if(position >= 0) {
                emailList = emailList.dropLast(1)
                notifyItemRemoved(position)
            }
        }
    }

    fun updateEmails(newEmailList: List<EmailData>){
        emailList = newEmailList
        notifyDataSetChanged()
    }
}