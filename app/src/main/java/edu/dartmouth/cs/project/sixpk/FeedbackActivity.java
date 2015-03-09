package edu.dartmouth.cs.project.sixpk;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import edu.dartmouth.cs.project.sixpk.database.AbLog;
import edu.dartmouth.cs.project.sixpk.database.Workout;
import edu.dartmouth.cs.project.sixpk.database.WorkoutEntryDataSource;


public class FeedbackActivity extends Activity {
    public final static String TAG = "FeedbackActivity";
    private Context mContext;
    private ListView mFeedbackList;
    private FeedbackListAdapter mAdapter;    // implement to take a list of workouts and format display
    private WorkoutEntryDataSource dbHelper;
    private long mWorkoutID;
    private Workout mCurrWorkout;
    ArrayList<String> mCompletedExercises;
    private ArrayList<Integer> feedbackArrayList = new ArrayList<>();
    private int[] feebackArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        mContext = this;

        // Open database
        dbHelper = new WorkoutEntryDataSource(mContext);
        dbHelper.open();

        // Get reference to listview in feedback layout
        mFeedbackList = (ListView) findViewById(R.id.listViewFeedback);

        // Set adapter for custom listView
        mAdapter = new FeedbackListAdapter(mContext);

        // Get current workout
        mWorkoutID = getIntent().getLongExtra(Globals.WORKOUT_ID_KEY, 0);
        mCurrWorkout = dbHelper.fetchWorkoutByIndex(mWorkoutID);
        updateListView();
        mFeedbackList.setAdapter(mAdapter);

    }

    // From int[] of completed exercises, form Array of formatted workout strings
    public void updateListView() {
        mAdapter.clear();   // reset list

        final int[] exerciseList = mCurrWorkout.getExerciseIdList();
        int[] durationList = mCurrWorkout.getDurationList();

        mCompletedExercises = new ArrayList<String>();

        // Populate listview
        for (int i = 0; i < exerciseList.length; i++) {
            feedbackArrayList.add(5);
            mCompletedExercises.add(dbHelper.getNameById(exerciseList[i]) + ", " + Globals.formatDuration(durationList[i]));
        }

        mAdapter.addAll(mCompletedExercises);
    }

    public void onSaveClicked(View v) {
        // Save feedback to database
        feebackArray = convertToIntArray(feedbackArrayList);
        dbHelper.open();
        Workout workout = dbHelper.fetchWorkoutByIndex(mWorkoutID);
        int[] exerciseIdList = workout.getExerciseIdList();
        workout.setFeedBackList(feebackArray);
        for(int i=feebackArray.length-1; i>=0;i--){
            AbLog abLog = dbHelper.fetchAbLogByIdentifier(exerciseIdList[i]);
            int[] difficultyArray = abLog.getDifficultyArray();
            abLog.setDifficultyArray(shiftValue(difficultyArray, feebackArray[i]));
            dbHelper.updateAbLog(abLog.getId(), abLog);
        }
        dbHelper.updateWorkoutEntry(mWorkoutID, workout);
        dbHelper.close();
        Long frequentTime = findFrequentWorkoutTime();

        long hours = TimeUnit.MILLISECONDS.toHours(frequentTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(frequentTime);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(frequentTime);
        long minutesSeconds = minutes%60 * 60;
        long hoursSeconds = hours%24 * 3600;
        long totalSecondsPastMidnight = hoursSeconds + minutesSeconds + seconds%60;
        Log.d(TAG, "Delay seconds past midnight: " + totalSecondsPastMidnight);

        long currentMillis = Calendar.getInstance().getTimeInMillis();

        long currHours = TimeUnit.MILLISECONDS.toHours(currentMillis);
        long currMinutes = TimeUnit.MILLISECONDS.toMinutes(currentMillis);
        long currSeconds = TimeUnit.MILLISECONDS.toSeconds(currentMillis);
        long currMinutesSeconds = currMinutes%60 * 60;
        long currHoursSeconds = currHours%24 * 3600;
        long currTotalSecondsPastMidnight = currHoursSeconds + currMinutesSeconds + currSeconds%60;
        Log.d(TAG, "Current Seconds past midnight: " + currTotalSecondsPastMidnight);

        long delay;
        if(currTotalSecondsPastMidnight>totalSecondsPastMidnight){
            long diff = currTotalSecondsPastMidnight - totalSecondsPastMidnight;
            long secondsInDay = 24*60*60;
            delay = secondsInDay - diff;
        }
        else{
            delay = totalSecondsPastMidnight - currTotalSecondsPastMidnight;
        }
        Log.d(TAG, "Notification delay: " + delay);
        scheduleNotification(getNotification(), delay);
        Intent toHomeScreen = new Intent(this, MainActivity.class);
        startActivity(toHomeScreen);
    }

    private void scheduleNotification(Notification notification, long delay) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Come Workout Now!");
        builder.setContentText("It will hurt now - but you'll feel good later");
        builder.setSmallIcon(R.drawable.logo2);
        builder.setColor(getResources().getColor(R.color.back_blue));
        return builder.build();
    }

    // Returns the time in milliseconds to set notification. Ignore day, month, year, minutes,
    // and seconds to just set for hour
    public long findFrequentWorkoutTime() {
        dbHelper.open();
        ArrayList<Workout> workouts = dbHelper.fetchWorkoutEntries();
        dbHelper.close();
        int length = workouts.size();
        long[] test = new long[length];

        // Find time of each completed workout
        for (int i = 0; i < length; i++) {
            test[i] = workouts.get(i).getDateTime();
        }

        Log.d(TAG, Globals.findMostCommonDate(test) + "");
        return Globals.findMostCommonDate(test);
    }

    private int[] shiftValue(int[] values, int value){
        for(int i = values.length-1; i>0;i--){
            values[i] = values[i-1];
        }
        values[0] = value;
        return values;
    }

    private int[] convertToIntArray(ArrayList<Integer> al) {
        ArrayList<Integer> test = al;
        int[] new_list = new int[al.size()];
        for (int i = 0; i < new_list.length; i++) {
            new_list[i] = al.get(i).intValue();
        }
        return new_list;
    }

    public void onDeleteCLicked(View v) {
        // Ask user through dialog if they really want to quit without saving
        showDeleteDialog();
    }

    // Set up adapter for listview element to scroll through workout itinerary
    private class FeedbackListAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;

        public FeedbackListAdapter(Context context) {
            super(context, R.layout.list_textview_seekbar);
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView != null) {
                view = convertView;
            }
            else {
                view = mInflater.inflate(R.layout.list_textview_seekbar, parent, false);
            }

            SeekBar seekbar = (SeekBar) view.findViewById(R.id.seekBar1);

            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    feedbackArrayList.set(position, (progress / 10));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });


            String text = getItem(position);
            ((TextView)view.findViewById(R.id.textViewWorkoutSeek)).setText(text);

            return view;
        }
    }

    // Show Dialog fragment to demo of workout from listView click
    public void showDeleteDialog() {
        DialogFragment newFragment = FeedbackDeleteFragment.newInstance();
        newFragment.show(getFragmentManager(), "dialog");
    }

    // Dialog to make sure the user wants to quit the completed workout without saving
    public static class FeedbackDeleteFragment extends DialogFragment {

        public static FeedbackDeleteFragment newInstance() {
            FeedbackDeleteFragment frag = new FeedbackDeleteFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.logo1)  // will need to get image associated with passed position
                    .setTitle(R.string.feedback_dialog_delete)
                    .setPositiveButton(R.string.feedback_dialog_no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    dismiss();  // close dialog
                                }
                            })
                    .setNegativeButton(R.string.feedback_dialog_yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    ((FeedbackActivity) getActivity())
                                            .doNegativeClick();
                                }
                            }).create();
        }
    }

    private void doNegativeClick() {
        // Return to start without saving workout
        dbHelper.removeWorkoutEntry(mWorkoutID);
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    @Override
    protected void onPause() {
        dbHelper.close();
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        dbHelper.open();
    }
}
