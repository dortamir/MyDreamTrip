package com.example.mydreamtrip.ui.explore

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.R
import com.example.mydreamtrip.data.repo.PostsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.mydreamtrip.ui.explore.GridSpacingItemDecoration

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var repo: PostsRepository
    private lateinit var pagingAdapter: DestinationPagingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = PostsRepository(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.rvDestinations)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        val spacing = resources.getDimensionPixelSize(R.dimen.space_8)
        rv.addItemDecoration(GridSpacingItemDecoration(2, spacing))
        rv.setHasFixedSize(true)

        pagingAdapter = DestinationPagingAdapter { dest ->
            val action = ExploreFragmentDirections
                .actionExploreFragmentToPostDetailsFragment(
                    postId = dest.id,
                    title = dest.title,
                    location = dest.location,
                    ratingText = dest.ratingText,
                    author = dest.author,
                    imageRes = dest.imageRes,
                    localImageUri = dest.localImageUri ?: "",
                    wikiTitle = dest.wikiTitle,
                    wikiExtract = dest.wikiExtract,
                    wikiUrl = dest.wikiUrl,
                    wikiImageUrl = dest.wikiImageUrl
                )
            findNavController().navigate(action)
        }
        rv.adapter = pagingAdapter

        val txtCount = view.findViewById<TextView>(R.id.txtCount)
        pagingAdapter.addLoadStateListener { state ->
            val loading = state.refresh is LoadState.Loading
            txtCount.text = if (loading) "Loading..." else "${pagingAdapter.itemCount} posts"
        }

        repo.startSyncExplorePosts()

        viewLifecycleOwner.lifecycleScope.launch {
            repo.explorePaging().collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }
    }
}
