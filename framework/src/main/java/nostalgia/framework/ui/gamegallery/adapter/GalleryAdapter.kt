package nostalgia.framework.ui.gamegallery.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SectionIndexer
import android.widget.TextView
import com.blankj.utilcode.util.Utils
import nostalgia.framework.R
import nostalgia.framework.data.database.GameDescription
import nostalgia.framework.data.entity.RowItem
import java.util.Arrays
import java.util.Collections
import java.util.Locale

class GalleryAdapter(private val context: Context) : BaseAdapter(), SectionIndexer {
    enum class SORT_TYPES {
        SORT_BY_NAME_ALPHA {
            override val tabName: String
                get() = names[0]
        },
        SORT_BY_INSERT_DATE {
            override val tabName: String
                get() = names[1]
        },
        SORT_BY_MOST_PLAYED {
            override val tabName: String
                get() = names[2]
        },
        SORT_BY_LAST_PLAYED {
            override val tabName: String
                get() = names[3]
        };

        abstract val tabName: String
    }

    private val alphaIndexer = HashMap<Char, Int>()
    private var filter = ""
    private var sections: Array<Char?> = emptyArray()
    private val inflater: LayoutInflater
    private val mainColor: Int
    private var games = ArrayList<GameDescription>()
    private val filterGames = ArrayList<RowItem>()
    private var sumRuns = 0
    private var sortType = SORT_TYPES.SORT_BY_NAME_ALPHA
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
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mainColor = context.resources.getColor(R.color.main_color)
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

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val (game) = filterGames[position]
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_game_list, null)
        }
        val name = convertView.findViewById<TextView>(R.id.row_game_item_name)
        val arrowIcon = convertView.findViewById<ImageView>(R.id.game_item_arrow)
        val bck = convertView.findViewById<ImageView>(R.id.game_item_bck)
        val runIndicator = convertView.findViewById<ProgressBar>(R.id.row_game_item_progressBar)
        runIndicator.max = sumRuns
        name.text = game!!.cleanName
        arrowIcon.setImageResource(R.drawable.ic_next_arrow)
        arrowIcon.clearAnimation()
        name.setTextColor(mainColor)
        name.gravity = Gravity.CENTER_VERTICAL
        bck.setImageResource(R.drawable.game_item_small_bck)
        return convertView
    }

    fun setFilter(filter: String) {
        this.filter = filter.lowercase(Locale.getDefault())
        filterGames()
    }

    fun setGames(games: ArrayList<GameDescription>?) {
        this.games = ArrayList(games)
        filterGames()
    }

    fun addGames(newGames: ArrayList<GameDescription>): Int {
        for (game in newGames) {
            if (!games.contains(game)) {
                games.add(game)
            }
        }
        filterGames()
        return games.size
    }

    private fun filterGames() {
        filterGames.clear()
        when (sortType) {
            SORT_TYPES.SORT_BY_NAME_ALPHA -> Collections.sort(games, nameComparator)
            SORT_TYPES.SORT_BY_INSERT_DATE -> Collections.sort(games, insertDateComparator)
            SORT_TYPES.SORT_BY_MOST_PLAYED -> Collections.sort(games, playedCountComparator)
            SORT_TYPES.SORT_BY_LAST_PLAYED -> Collections.sort(games, lastPlayedDateComparator)
        }
        val containsFilter = " $filter"
        sumRuns = 0
        for (game in games) {
            sumRuns = if (game.runCount > sumRuns) game.runCount else sumRuns
            val name = game.cleanName.lowercase(Locale.getDefault())
            var secondCondition = true
            if (sortType === SORT_TYPES.SORT_BY_LAST_PLAYED || sortType === SORT_TYPES.SORT_BY_MOST_PLAYED) {
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
        if (sortType === SORT_TYPES.SORT_BY_NAME_ALPHA) {
            for (i in filterGames.indices) {
                val (_, ch) = filterGames[i]
                if (!alphaIndexer.containsKey(ch)) {
                    alphaIndexer[ch] = i
                }
            }
        }
        super.notifyDataSetChanged()
    }

    fun setSortType(sortType: SORT_TYPES) {
        this.sortType = sortType
        filterGames()
    }

    override fun getPositionForSection(section: Int): Int {
        return try {
            val ch = sections[section]?.lowercaseChar()
            val pos = alphaIndexer[ch]
            pos ?: 0
        } catch (e: ArrayIndexOutOfBoundsException) {
            0
        }
    }

    override fun getSectionForPosition(position: Int): Int {
        val (_, firstLetter) = getItem(position) as RowItem
        val ch = firstLetter.uppercaseChar()
        for (i in sections.indices) {
            val ch1 = sections[i]
            if (ch1 == ch) {
                return i
            }
        }
        return 1
    }

    override fun getSections(): Array<Char?> {
        val keyset: Set<Char> = alphaIndexer.keys
        sections = arrayOfNulls(keyset.size)
        Arrays.sort(sections) { obj: Char?, anotherCharacter: Char? ->
            obj!!.compareTo(
                anotherCharacter!!
            )
        }
        for (i in sections.indices) sections[i] = sections[i]?.uppercaseChar()
        return sections
    }

    override fun notifyDataSetChanged() {
        filterGames()
    }

    companion object {
        private val names = Utils.getApp().resources.getStringArray(R.array.gallery_page_tab_names)
    }
}