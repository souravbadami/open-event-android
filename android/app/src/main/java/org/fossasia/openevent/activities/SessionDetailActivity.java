package org.fossasia.openevent.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.SpeakersListAdapter;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.data.Speaker;
import org.fossasia.openevent.dbutils.DbSingleton;
import org.fossasia.openevent.receivers.NotificationAlarmReceiver;
import org.fossasia.openevent.utils.ConstantStrings;
import org.fossasia.openevent.utils.ISO8601Date;
import org.fossasia.openevent.widget.BookmarkWidgetProvider;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import timber.log.Timber;

/**
 * User: MananWason
 * Date: 08-07-2015
 */
public class SessionDetailActivity extends BaseActivity {
    private static final String TAG = "Session Detail";

    private SpeakersListAdapter adapter;

    private Session session;

    private String timings;
    private String FRAGMENT_TAG_REST = "fgtr";

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.title_session)
    protected TextView text_title;
    @BindView(R.id.subtitle_session)
    protected TextView text_subtitle;
    @BindView(R.id.date_session)
    protected TextView text_date;
    @BindView(R.id.start_time_session)
    protected TextView text_start_time;
    @BindView(R.id.end_time_session)
    protected TextView text_end_time;
    @BindView(R.id.track)
    protected TextView text_track;
    @BindView(R.id.tv_location)
    protected TextView text_room1;
    @BindView(R.id.tv_abstract_text)
    protected TextView summary;
    @BindView(R.id.tv_description)
    protected TextView descrip;
    @BindView(R.id.list_speakerss)
    protected RecyclerView speakersRecyclerView;
    @BindView(R.id.fab_session_bookmark)
    protected FloatingActionButton fabSessionBookmark;

    private String trackName, title;

    private Spanned result;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DbSingleton dbSingleton = DbSingleton.getInstance();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        title = getIntent().getStringExtra(ConstantStrings.SESSION);
        trackName = getIntent().getStringExtra(ConstantStrings.TRACK);
        Timber.tag(TAG).d(title);

        final List<Speaker> speakers = dbSingleton.getSpeakersbySessionName(title);
        session = dbSingleton.getSessionbySessionname(title);

        text_room1.setText((dbSingleton.getMicrolocationById(session.getMicrolocation().getId())).getName());

        text_title.setText(title);
        if (session.getSubtitle().equals("")){
            text_subtitle.setVisibility(View.GONE);
        }
        text_subtitle.setText(session.getSubtitle());
        text_track.setText(trackName);

        updateFloatingIcon(fabSessionBookmark);

        fabSessionBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DbSingleton dbSingleton = DbSingleton.getInstance();
                if (dbSingleton.isBookmarked(session.getId())) {
                    Timber.tag(TAG).d("Bookmark Removed");
                    dbSingleton.deleteBookmarks(session.getId());
                    fabSessionBookmark.setImageDrawable(ContextCompat.getDrawable(SessionDetailActivity.this, R.drawable.ic_bookmark_outline_white_24dp));
                    Toast.makeText(SessionDetailActivity.this, R.string.removed_bookmark, Toast.LENGTH_SHORT).show();
                } else {
                    Timber.tag(TAG).d("Bookmarked");
                    dbSingleton.addBookmarks(session.getId());
                    fabSessionBookmark.setImageDrawable(ContextCompat.getDrawable(SessionDetailActivity.this, R.drawable.ic_bookmark_white_24dp));
                    createNotification();
                    Toast.makeText(SessionDetailActivity.this, R.string.added_bookmark, Toast.LENGTH_SHORT).show();
                }
                sendBroadcast(new Intent(BookmarkWidgetProvider.ACTION_UPDATE));
            }
        });

        String date = ISO8601Date.getTimeZoneDateString(
                ISO8601Date.getDateObject(session.getStartTime())).split(",")[0] + ","
                + ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime())).split(",")[1];
        String startTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime()));
        String endTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getEndTime()));

        if (TextUtils.isEmpty(startTime) && TextUtils.isEmpty(endTime)) {
            text_start_time.setText(R.string.time_not_specified);
            text_end_time.setVisibility(View.GONE);

        } else {
            text_start_time.setText(startTime);
            text_end_time.setText(endTime);
            text_date.setText(date);

        }
        summary.setText(session.getSummary());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(session.getDescription(), Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(session.getDescription());
        }
        descrip.setText(result);

        adapter = new SpeakersListAdapter(speakers, this);

        speakersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        speakersRecyclerView.setAdapter(adapter);
        speakersRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void updateFloatingIcon(FloatingActionButton fabSessionBookmark) {
        DbSingleton dbSingleton = DbSingleton.getInstance();
        if (dbSingleton.isBookmarked(session.getId())) {
            Timber.tag(TAG).d("Bookmarked");
            fabSessionBookmark.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_bookmark_white_24dp));
        } else {
            Timber.tag(TAG).d("Bookmark Removed");
            fabSessionBookmark.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_bookmark_outline_white_24dp));
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_sessions_detail;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map:
                /** Hide all the views except the frame layout **/
                NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.nested_scrollview_session_detail);
                scrollView.setVisibility(View.GONE);
                AppBarLayout sessionDetailAppBar = (AppBarLayout) findViewById(R.id.app_bar_session_detail);
                sessionDetailAppBar.setVisibility(View.GONE);
                fabSessionBookmark.setVisibility(View.GONE);

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.content_frame_session,
                        ((OpenEventApp) getApplication())
                                .getMapModuleFactory()
                                .provideMapModule()
                                .provideMapFragment(), FRAGMENT_TAG_REST).commit();
                return true;

            case R.id.action_share:
                String startTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime()));
                String endTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getEndTime()));
                StringBuilder shareText = new StringBuilder();
                shareText.append(String.format("Session Track: %s \nTitle: %s \nStart Time: %s \nEnd Time: %s\n",
                        trackName, title, startTime, endTime));
                if (!result.toString().isEmpty()){
                    shareText.append("\nDescription: ").append(result.toString());
                }
                else{
                    shareText.append(getString(R.string.descriptionEmpty));
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_links)));
                return true;

            case R.id.action_add_to_calendar:
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra(CalendarContract.Events.TITLE, title);
                intent.putExtra(CalendarContract.Events.DESCRIPTION, session.getDescription());
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ISO8601Date.getDateObject(session.getStartTime()).getTime());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        ISO8601Date.getDateObject(session.getEndTime()).getTime());
                startActivity(intent);

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_session_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }


    public void createNotification() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ISO8601Date.getTimeZoneDate(ISO8601Date.getDateObject(session.getStartTime())));

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Integer pref_result = Integer.parseInt(sharedPrefs.getString("notification", "10 mins").substring(0, 2).trim());
        if (pref_result.equals(1)) {
            calendar.add(Calendar.HOUR, -1);
        } else if (pref_result.equals(12)) {
            calendar.add(Calendar.HOUR, -12);
        } else {
            calendar.add(Calendar.MINUTE, -10);
        }
        Intent myIntent = new Intent(this, NotificationAlarmReceiver.class);
        myIntent.putExtra(ConstantStrings.SESSION, session.getId());
        myIntent.putExtra(ConstantStrings.SESSION_TIMING, timings);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }
}