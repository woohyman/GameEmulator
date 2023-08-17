package nostalgia.framework.ui.gamegallery;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.blankj.utilcode.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import nostalgia.framework.R;
import nostalgia.framework.utils.NLog;

public class AppStoreAdapter extends BaseAdapter implements SectionIndexer {

    private static final String[] names = Utils.getApp().getResources().getStringArray(R.array.gallery_page_tab_names);

    enum SORT_TYPES {
        SORT_BY_NAME_ALPHA {
            public String getTabName() {
                return names[0];
            }
        },
        SORT_BY_INSERT_DATE {
            public String getTabName() {
                return names[1];
            }
        },
        SORT_BY_MOST_PLAYED {
            public String getTabName() {
                return names[2];
            }
        },
        SORT_BY_LAST_PLAYED {
            public String getTabName() {
                return names[3];
            }
        },
        DOWNLOAD_BY_REMOTE {
            public String getTabName() {
                return names[4];
            }
        };

        public abstract String getTabName();
    }

    private HashMap<Character, Integer> alphaIndexer = new HashMap<>();
    private String filter = "";
    private Character[] sections;
    private LayoutInflater inflater;
    private Context context;
    private int mainColor;
    private ArrayList<GameDescription> games = new ArrayList<>();
    private ArrayList<RowItem> filterGames = new ArrayList<>();
    private int sumRuns = 0;
    private SORT_TYPES sortType = SORT_TYPES.DOWNLOAD_BY_REMOTE;

    private Comparator<GameDescription> nameComparator = (lhs, rhs) ->
            lhs.getSortName().compareTo(rhs.getSortName());

    private Comparator<GameDescription> insertDateComparator = (lhs, rhs) ->
            (int) (-lhs.inserTime + rhs.inserTime);

    private Comparator<GameDescription> lastPlayedDateComparator = (lhs, rhs) -> {
        long dif = lhs.lastGameTime - rhs.lastGameTime;
        if (dif == 0) {
            return 0;
        } else if (dif < 0) {
            return 1;
        } else {
            return -1;
        }
    };
    private Comparator<GameDescription> playedCountComparator = (lhs, rhs) ->
            -lhs.runCount + rhs.runCount;

    public AppStoreAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mainColor = context.getResources().getColor(R.color.main_color);
        GameDescription gameDescription = new GameDescription();
        gameDescription.name = "坦克";
        games.add(gameDescription);

        for (GameDescription game : games) {
            sumRuns = game.runCount > sumRuns ? game.runCount : sumRuns;
            String name = game.getCleanName().toLowerCase();
            RowItem item = new RowItem();
            item.game = game;
            item.firstLetter = name.charAt(0);
            filterGames.add(item);
        }
    }

    @Override
    public int getCount() {
        return filterGames.size();
    }

    @Override
    public Object getItem(int position) {
        return filterGames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowItem item = filterGames.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_game_list, null);
        }
        GameDescription game = item.game;
        TextView name = convertView.findViewById(R.id.row_game_item_name);
        ImageView arrowIcon = convertView.findViewById(R.id.game_item_arrow);
        ImageView bck = convertView.findViewById(R.id.game_item_bck);
        ProgressBar runIndicator = convertView.findViewById(R.id.row_game_item_progressBar);
        runIndicator.setMax(sumRuns);
        name.setText(game.getCleanName());
        arrowIcon.setImageResource(R.drawable.ic_next_arrow);
        arrowIcon.clearAnimation();
        name.setTextColor(mainColor);
        name.setGravity(Gravity.CENTER_VERTICAL);
        bck.setImageResource(R.drawable.game_item_small_bck);
        return convertView;
    }

    @Override
    public int getPositionForSection(int section) {
        try {
            Character ch = Character.toLowerCase(sections[section]);
            Integer pos = alphaIndexer.get(ch);
            if (pos == null) {
                return 0;
            } else {
                return pos;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        RowItem item = (RowItem) getItem(position);
        char ch = Character.toUpperCase(item.firstLetter);
        for (int i = 0; i < sections.length; i++) {
            Character ch1 = sections[i];
            if (ch1.equals(ch)) {
                return i;
            }
        }
        return 1;
    }

    @Override
    public Object[] getSections() {
        Set<Character> keyset = alphaIndexer.keySet();
        sections = new Character[keyset.size()];
        keyset.toArray(sections);
        Arrays.sort(sections, Character::compareTo);
        for (int i = 0; i < sections.length; i++)
            sections[i] = Character.toUpperCase(sections[i]);
        return sections;
    }

    @Override
    public void notifyDataSetChanged() {

    }

    public class RowItem {
        GameDescription game;
        char firstLetter;
    }

}