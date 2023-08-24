package com.woohyman.xml.ui.gamegallery.base

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SectionIndexer
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.RowItem
import com.woohyman.xml.R
import com.woohyman.xml.databinding.RowGameListBinding
import com.woohyman.xml.ui.gamegallery.model.SortType
import com.woohyman.xml.ui.gamegallery.model.TabInfo
import java.util.Locale

open class BaseGalleryAdapter(activity: AppCompatActivity) : BaseAdapter(), SectionIndexer {
    private val alphaIndexer = HashMap<Char, Int>()
    private var filter = ""
    private var sections: Array<Char?> = emptyArray()
    private val inflater: LayoutInflater
    private val mainColor: Int
    var games = ArrayList<GameDescription>()
        set(value) {
            field = value
            filterGames()
        }
    protected val filterGames = ArrayList<RowItem>()
    private var sumRuns = 0
    private var sortType: TabInfo? = null
    private val nameComparator =
        java.util.Comparator { lhs: GameDescription, rhs: GameDescription ->
            lhs.sortName.compareTo(rhs.sortName)
        }
    private val insertDateComparator =
        java.util.Comparator { lhs: GameDescription, rhs: GameDescription -> (-lhs.inserTime + rhs.inserTime).toInt() }
    private val lastPlayedDateComparator =
        java.util.Comparator { lhs: GameDescription, rhs: GameDescription ->
            val dif = lhs.lastGameTime - rhs.lastGameTime
            if (dif == 0L) {
                return@Comparator 0
            } else if (dif < 0) {
                return@Comparator 1
            } else {
                return@Comparator -1
            }
        }
    private val playedCountComparator =
        java.util.Comparator { lhs: GameDescription, rhs: GameDescription -> -lhs.runCount + rhs.runCount }

    init {
        inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mainColor = activity.resources.getColor(R.color.main_color)
    }

    override fun getCount(): Int {
        return filterGames.size
    }

    override fun getItem(position: Int): Any {
        return filterGames[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val (game) = filterGames[position]
        val view = convertView ?: inflater.inflate(R.layout.row_game_list, null)
        val binding = RowGameListBinding.bind(view)
        binding.rowGameItemProgressBar.max = sumRuns
        binding.rowGameItemName.text = game!!.cleanName
        binding.gameItemArrow.setImageResource(R.drawable.ic_next_arrow)
        binding.gameItemArrow.clearAnimation()
        binding.rowGameItemName.setTextColor(mainColor)
        binding.rowGameItemName.gravity = Gravity.CENTER_VERTICAL
        binding.gameItemBck.setImageResource(R.drawable.game_item_small_bck)
        return view
    }

    fun setFilter(filter: String) {
        this.filter = filter.lowercase(Locale.getDefault())
        filterGames()
    }

    fun addGames(newGames: ArrayList<GameDescription>): Int {
        newGames.forEach {
            if (!games.contains(it)) {
                games.add(it)
            }
        }
        filterGames()
        return games.size
    }

    private fun filterGames() {
        filterGames.clear()
        val containsFilter = " $filter"
        sumRuns = 0
        for (game in games) {
            sumRuns = if (game.runCount > sumRuns) game.runCount else sumRuns
            val name = game.cleanName.lowercase(Locale.getDefault())
            var secondCondition = true
            if (sortType?.sortType === SortType.SORT_BY_LAST_PLAYED || sortType?.sortType === SortType.SORT_BY_MOST_PLAYED) {
                secondCondition = game.lastGameTime != 0L
            }
            if ((name.startsWith(filter) || name.contains(containsFilter)) and secondCondition) {
                val item = RowItem()
                item.game = game
                item.firstLetter = name[0]
                filterGames.add(item)
            }
        }
        alphaIndexer.clear()
        if (sortType?.sortType === SortType.SORT_BY_NAME_ALPHA) {
            for (i in filterGames.indices) {
                val (_, ch) = filterGames[i]
                if (!alphaIndexer.containsKey(ch)) {
                    alphaIndexer[ch] = i
                }
            }
        }
        super.notifyDataSetChanged()
    }

    fun setSortType(sortType: TabInfo) {
        this.sortType = sortType
        filterGames()
    }

    override fun getPositionForSection(section: Int): Int {
        val ch = sections[section]?.lowercaseChar()
        return alphaIndexer[ch] ?: 0
    }

    override fun getSectionForPosition(position: Int): Int {
        val (_, firstLetter) = getItem(position) as RowItem
        sections.forEachIndexed { index, c ->
            if (c == firstLetter.uppercaseChar()) {
                return index
            }
        }
        return 1
    }

    override fun getSections(): Array<Char?> {
        sections = emptyArray()
        alphaIndexer.keys.sortedWith { o1, o2 ->
            o1.compareTo(o2)
        }.map {
            it.uppercaseChar()
        }.forEach {
            sections.plus(it)
        }
        return sections
    }

    override fun notifyDataSetChanged() {
        filterGames()
    }
}