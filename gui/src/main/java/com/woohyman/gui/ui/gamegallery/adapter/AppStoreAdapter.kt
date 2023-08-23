package com.woohyman.gui.ui.gamegallery.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import com.blankj.utilcode.util.Utils
import com.woohyman.gui.R
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.RowItem
import java.util.Arrays
import java.util.Locale

class AppStoreAdapter(private val context: Context) : BaseAdapter(), SectionIndexer {
    internal enum class SORT_TYPES {
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
        },
        DOWNLOAD_BY_REMOTE {
            override val tabName: String
                get() = names[4]
        };

        abstract val tabName: String
    }

    private val alphaIndexer = HashMap<Char, Int>()
    private val filter = ""
    private var sections: Array<Char?> = emptyArray()
    private val inflater: LayoutInflater
    private val mainColor: Int
    private val games = ArrayList<GameDescription>()
    private val filterGames = ArrayList<RowItem>()
    private var sumRuns = 0
    private val sortType = SORT_TYPES.DOWNLOAD_BY_REMOTE
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
        val gameDescription = GameDescription()
        gameDescription.url =
            "https://gitee.com/popvan/nes-repo/raw/master/roms/Super%20Mario%20Bros.%203.nes"
        gameDescription.name = "超级马里奥兄弟3"
        games.add(gameDescription)
        for (game in games) {
            sumRuns = if (game.runCount > sumRuns) game.runCount else sumRuns
            val name = game.cleanName.lowercase(Locale.getDefault())
            val item = RowItem()
            item.game = game
            item.firstLetter = name[0]
            filterGames.add(item)
        }
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
        return convertView ?: inflater.inflate(R.layout.row_game_list, null).also {
            val (game) = filterGames[position]
            val name = it.findViewById<TextView>(R.id.row_game_item_name)
            val arrowIcon = it.findViewById<ImageView>(R.id.game_item_arrow)
            val bck = it.findViewById<ImageView>(R.id.game_item_bck)
            name.text = game!!.cleanName
            arrowIcon.setImageResource(R.drawable.ic_next_arrow)
            arrowIcon.clearAnimation()
            name.setTextColor(mainColor)
            name.gravity = Gravity.CENTER_VERTICAL
            bck.setImageResource(R.drawable.game_item_small_bck)
        }
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
//        keyset.toArray<Char>(sections)
        Arrays.sort(sections) { obj: Char?, anotherCharacter: Char? ->
            obj!!.compareTo(
                anotherCharacter!!
            )
        }
        for (i in sections.indices) sections[i] = sections[i]?.uppercaseChar()
        return sections
    }

    override fun notifyDataSetChanged() {}

    companion object {
        private val names = Utils.getApp().resources.getStringArray(R.array.gallery_page_tab_names)
    }
}