package edu.dartmouth.cs.project.sixpk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import edu.dartmouth.cs.project.sixpk.database.AbLog;
import edu.dartmouth.cs.project.sixpk.database.MySQLiteHelper;
import edu.dartmouth.cs.project.sixpk.database.Workout;
import edu.dartmouth.cs.project.sixpk.database.WorkoutEntryDataSource;


public class PreviewActivity extends Activity{//extends ListActivity {
    private EditText mDuration;
    private EditText mDifficulty;
    private Context mContext;
    private ListView mItineraryList;
    private ItineraryListAdapter mAdapter;    // implement to take a list of workouts and format display

    private WorkoutEntryDataSource dbHelper;
    private int[] mExerciseList;
    private int[] mDifficultyList;
    ArrayList<String> mExerciseItinerary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preview);
        mContext = this;
        dbHelper = new WorkoutEntryDataSource(mContext);
        dbHelper.open();

        // Get text references
        mDuration = (EditText) findViewById(R.id.editTextPrevItinerary);
        mDifficulty = (EditText) findViewById(R.id.editTextPrevDifficulty);
        mItineraryList = (ListView) findViewById(R.id.listViewPreview);

        // Set adapter for custom listView
//        mAdapter = new ItineraryListAdapter(mContext);
//        mItineraryList.setAdapter(mAdapter);

        // Set adapter for test listView -- TEMPORARY
//        mAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, android.R.id.text1, Globals.test);
        mItineraryList.setAdapter(mAdapter);

        // Set onClick listener for the listView, show dialog on click
        mItineraryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position,
                                    long id) {
                //String title = listViewList.get(position).getWorkoutTitle();
                String title = "test workout: " + position;
                showWorkoutDialog(title, position);
            }
        });

        // Unpack intent
        int difficulty = getIntent().getIntExtra(Globals.WORKOUT_DIFFICULTY_KEY, Globals.WORKOUT_MED);
        String diff = "MEDIUM"; // DEFAULT: medium

        switch (difficulty) {
            case Globals.WORKOUT_EASY:
                diff = "EASY";
                break;
            case Globals.WORKOUT_MED:
                diff = "MEDIUM";
                break;
            case Globals.WORKOUT_HARD:
                diff = "HARD";
                break;
        }

        int time = getIntent().getIntExtra(Globals.WORKOUT_DURATION_KEY, Globals.DEFAULT_TIME);

        // Set text views
        mDuration.setText(time + " min");
        mDifficulty.setText(diff);


        ArrayList<AbLog> allExercises = dbHelper.fetchAbLogEntries();
        Workout w = new Workout(allExercises, time, difficulty);
        final int[] mExerciseList = w.getExerciseIdList();
        int[] mDurationList = w.getDurationList();

        mExerciseItinerary = new ArrayList<String>();

        // Populate listview
        for (int i : mExerciseList) {
            // TODO: need getNameById function
          //  mExerciseItinerary.add(getExerciseNameById(mExerciseList[i]) + ", " + Globals.formatDuration(mDurationList[i]));

            // TEMPORARY
            mExerciseItinerary.add(mExerciseList[i] + ", " +Globals.formatDuration(mDurationList[i]));
        }
        mAdapter.addAll(mExerciseItinerary);


        // Get text references
        mDuration = (EditText) findViewById(R.id.editTextPrevItinerary);
        mDifficulty = (EditText) findViewById(R.id.editTextPrevDifficulty);
        mItineraryList = (ListView) findViewById(R.id.listViewPreview);

        // Set adapter for custom listView
        mAdapter = new ItineraryListAdapter(mContext);
        mItineraryList.setAdapter(mAdapter);

        // Set onClick listenter for the listView, show dialog on click
        mItineraryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position,
                                    long id) {
                //String title = listViewList.get(position).getWorkoutTitle();
                String title = "workout " + mExerciseList[position] + ": " + position;
                showWorkoutDialog(title, position);
            }
        });
    }

    public void onStartClicked(View v) {
        Intent i = new Intent(this, WorkoutActivity.class);
        startActivity(i);
    }

    public void onCancelClicked(View v) {
        finish();
    }

   // Set up adapter for listview element to scroll through workout itinerary
    private class ItineraryListAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;

        public ItineraryListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView != null) {
                view = convertView;
            }
            else {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            String text = getItem(position);
            ((TextView)view.findViewById(android.R.id.text1)).setText(text);

            return view;
        }
    }

    // Show Dialog fragment to demo of workout from listView click
    public void showWorkoutDialog(String workoutTitle, int position) {
        DialogFragment newFragment = WorkoutDemoFragment.newInstance(workoutTitle, position);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public static class WorkoutDemoFragment extends DialogFragment {

        public static WorkoutDemoFragment newInstance(String title, int position) {
            WorkoutDemoFragment frag = new WorkoutDemoFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putInt("position", position);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            final int position = getArguments().getInt("position");

            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.logo1)  // will need to get image associated with passed position
                    .setTitle(title)
                    .setPositiveButton(R.string.workout_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    dismiss();
                                }
                            })
                    .setNegativeButton(R.string.alert_dialog_delete,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    ((PreviewActivity) getActivity())
                                            .doNegativeClick(position);
                                }
                            }).create();
        }
    }

    // Delete workout from itinerary
    private void doNegativeClick(int index) {
        // delete workout from itinerary
        // TEMPORARY: change to the actual itinerary list (mExerciseList)
        Globals.test.remove(index);

        //TODO need removeExercise function in Workout
        // given an index, remove that index from the exerciseList and durationList
        mAdapter.notifyDataSetChanged();    // update UI view of list
    }

    @Override
    protected void onPause() {
        dbHelper.close();
        super.onPause();
    }
}
