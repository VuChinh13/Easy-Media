package com.example.easymedia.ui.component.home.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.AuthRepository
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.databinding.ItemFirstStoryBinding
import com.example.easymedia.databinding.StoryItemBinding
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import com.example.easymedia.ui.component.utils.SharedPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryAdapter(
    val listStory: MutableList<Story>,
    val listenerStory: OnAvatarClickListener,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var context: Context
    private var myStory = false
    private var storyOfUser = mapOf<String, List<Story>>()
    private var listSize = listStory.size
    private val userId = SharedPrefer.getId()
    private val repo: AuthRepository =
        AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))

    companion object {
        private const val VIEW_TYPE_FIRST = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    class FirstStoryViewHolder(binding: ItemFirstStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val constraintLayout = binding.cl1
        val ivStory = binding.ivUserStory
        val btnAddStory = binding.btnAddStory
    }

    class NormalStoryViewHolder(binding: StoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivStory = binding.ivUserStory
        val txtUsername = binding.txtUsername
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FIRST else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return if (viewType == VIEW_TYPE_FIRST) {
            val binding =
                ItemFirstStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FirstStoryViewHolder(binding)
        } else {
            val binding =
                StoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            NormalStoryViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        with(holder) {

            if (this is FirstStoryViewHolder) {
                // hiển thị story của mình
                if (!myStory) {
                    constraintLayout.background = null
                }
                val profilePicture = SharedPrefer.getProfilePicture()
                Glide.with(itemView.context)
                    .load(profilePicture)
                    .error(R.drawable.ic_avatar)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(ivStory)
                btnAddStory.setOnClickListener {
                    Log.d("ok", "ok day chu")
                    // chuyển màn hình
                    listenerStory.onStoryClick()
                }
                // nếu mà có story của mình -> xem được
                if (myStory) {
                    itemView.setOnClickListener {
                        listenerStory.switchScreenStory(storyOfUser[userId] ?: emptyList())
                    }
                }
            } else if (this is NormalStoryViewHolder) {
                val index = if (myStory) {
                    position
                } else {
                    position - 1
                }
                lifecycleCoroutineScope.launch(Dispatchers.IO) {
                    val result = repo.getUserById(listStory[index].userId)
                    result.onSuccess { user ->
                        withContext(Dispatchers.Main) {
                            Log.d("checkStory", user.toString())
                            if (user != null) {
                                Glide.with(itemView.context)
                                    .load(user.profilePicture)
                                    .error(R.drawable.ic_avatar)
                                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                    .into(ivStory)
                                txtUsername.text = user.username
                            }
                        }
                    }
                }
                itemView.setOnClickListener {
                    listenerStory.switchScreenStory(
                        storyOfUser[listStory[index].userId] ?: emptyList()
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = listSize

    @SuppressLint("NotifyDataSetChanged")
    fun updateListStory(newList: List<Story>) {
        listStory.clear()
        // lọc story theo id người dùng
        storyOfUser = newList.groupBy { it.userId }
        val uniqueStories = newList.distinctBy { it.userId }
        // lọc ra các người dùng mà ko bị trùng
        // đưa lên trên đầu
        val filter = uniqueStories.sortedByDescending { it.userId == userId }
        myStory = filter[0].userId == userId
        listSize = if (!myStory) {
            filter.size + 1
        } else filter.size
        listStory.addAll(filter)
        notifyDataSetChanged()
    }
}