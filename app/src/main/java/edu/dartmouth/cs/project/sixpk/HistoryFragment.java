package edu.dartmouth.cs.project.sixpk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryFragment extends Fragment {
    private Button showStats;
    private Context mContext;
    private ListView mHistoryList;
    private HistoryListAdapter mAdapter;    // implement to take a list of workouts and format display

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the view in the start xml
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mContext = getActivity();

        // Get reference to listview in feedback layout
        mHistoryList = (ListView) view.findViewById(R.id.listViewHistory);

        // Show listview of past workouts
        // TODO
        // Set adapter for custom listView
        mAdapter = new HistoryListAdapter(mContext);
        mHistoryList.setAdapter(mAdapter);
        mAdapter.addAll(Globals.test);  // TEMPORARY: get all workouts from database
        mAdapter.notifyDataSetChanged();

        // Start workout preview
        showStats = (Button) view.findViewById(R.id.buttonShowStats);
        showStats.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Go to statisticsActivity to see data visualization of workout history
                Intent i = new Intent(getActivity(), StatisticsActivity.class);
                startActivity(i);
            }
        });
        return view;
    }

        private class HistoryListAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;

        public HistoryListAdapter(Context context) {
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

          //  WorkoutObject w = getItem(position);
            String temp = getItem(position);
            ((TextView)view.findViewById(android.R.id.text1)).setText(temp);

            return view;
        }
    }

}