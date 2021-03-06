package com.fitnessapp.client.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.fitnessapp.client.BaseDrawerActivity;
import com.fitnessapp.client.R;
import com.fitnessapp.client.Utils.StaticStrings;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

public class ProgressTrackerFragment extends Fragment implements View.OnClickListener {

    private int nDays = 0;
    private LineChart mChart;
    private Boolean isPremium;
    private String workoutId = "", workoutDuration = "";
    private ArrayList<String> exercicesNames;
    private ArrayList<JSONObject> dailyExercices, dailyTrackers;
    private JSONObject user,workout;
    private UrlConnectorGetProgress ucgp;
    private UrlConnectorGetExercices ucge;
    private UrlConnectorPostTracker ucpt;
    private boolean isAdvancedWorkout;
    private URL url;
    private HttpURLConnection conn;
    private BaseDrawerActivity activity;
    private View rootView;
    private Button itButton;
    private Integer setsValue, repsValue;
    private Spinner repsSpinner,setsSpinner,exercicesSpinner;

    public ProgressTrackerFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Progress Tracker");
        dailyExercices = new ArrayList<>();
        exercicesNames = new ArrayList<>();
        dailyTrackers = new ArrayList<>();
        ucgp = new UrlConnectorGetProgress();
        ucge = new UrlConnectorGetExercices();
        ucpt = new UrlConnectorPostTracker();
        isAdvancedWorkout = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_progress_tracker, container, false);
        itButton = rootView.findViewById(R.id.itButton);
        itButton.setOnClickListener(this);
        activity = (BaseDrawerActivity)getActivity();
        ucge.execute();
        ucgp.execute();
        try {
            loadData(rootView);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rootView;
    }


    private void loadData(View rootView) throws JSONException {
        mChart = rootView.findViewById(R.id.linechart);

        setData();

        repsSpinner = rootView.findViewById(R.id.spinnerReps);
        setsSpinner = rootView.findViewById(R.id.spinnerSets);

        setsSpinner.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_item, getResources().getStringArray(R.array.setsArray)));
        repsSpinner.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_item, getResources().getStringArray(R.array.repsArray)));
    }

    private void setData() throws JSONException {
        mChart.invalidate();
        mChart.clear();

        ArrayList<Entry> yVals = setYAxisValues();

        ArrayList<String> xVals = setXAxisValues();

        LineDataSet set1 = new LineDataSet(yVals, "Burnt kcal");;
        set1.setLineWidth(3F);
        set1.setColor(Color.BLACK);
        set1.setValueTextColor(Color.BLACK);
        set1.setValueTextSize(15f);

        ArrayList<ILineDataSet> ld = new ArrayList<>();
        ld.add(set1);

        // create a data object with the datasets
        LineData data = new LineData(xVals, ld);
        // set data
        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.setDescription("");
        mChart.setData(data);
        mChart.invalidate();
    }

    private ArrayList<String> setXAxisValues() throws JSONException {
        ArrayList xVals = new ArrayList();
        String tmpDate = "";
        for (int j = 0; j < dailyTrackers.size(); j++){
            if(!dailyTrackers.get(j).getString("date").equals(tmpDate)){
                xVals.add(dailyTrackers.get(j).getString("date"));
                tmpDate = dailyTrackers.get(j).getString("date");
            }
        }
        System.out.println("xVal size: " + xVals.size());
        return xVals;
    }


    private ArrayList<Entry> setYAxisValues(){
        ArrayList<Entry> yVals = new ArrayList<Entry>();
        String tmpDate = "";
        String date = "";
        int tmpRep;
        int tmpSer;
        TreeMap<String, Integer> trackersByDay = new TreeMap<>();
        int totalKcal = 0;
        for(JSONObject tracker : dailyTrackers){
            try{
                date = tracker.getString("date");
                tmpSer = tracker.getInt("trackingSets");
                tmpRep = tracker.getInt("repetitions");
                totalKcal += (((tracker.getInt("kcal")*tmpSer)/4)/2)+(((tracker.getInt("kcal")*tmpRep)/10)/2);

                if(date.equals(tmpDate)){
                    trackersByDay.put(tmpDate, totalKcal);
                    tmpDate = tracker.getString("date");
                }else {
                    totalKcal = 0;
                    trackersByDay.put(date, tracker.getInt("kcal"));
                    tmpDate = date;
                    totalKcal += (((tracker.getInt("kcal")*tmpSer)/4)/2)+(((tracker.getInt("kcal")*tmpRep)/10)/2);
                    nDays++;
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        int counter = 0;
        for(TreeMap.Entry<String, Integer> entry : trackersByDay.entrySet() ){
            yVals.add(new Entry(Float.parseFloat(entry.getValue().toString()), counter));
            counter++;
        }
        System.out.println("yVal size:" + yVals.size());
        return yVals;
    }

    public void setExerciceSpinnerData(View rootView){
        exercicesSpinner = rootView.findViewById(R.id.spinnerRoutines);
        exercicesSpinner.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_item, this.exercicesNames));
    }

    @Override
    public void onClick(View view) {
        if(dailyExercices.size() > 0) {
            ucpt = new UrlConnectorPostTracker();
            ucpt.execute();
        }else{
            //REST DAY
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Today is a rest day!")
                    .setMessage("You can not add any training if today is a rest day.")
                    .setNegativeButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private class UrlConnectorGetExercices extends AsyncTask<Void,Void,Void> {


        @Override
        protected Void doInBackground(Void... params) {
            try {

                URL url = new URL(StaticStrings.ipserver + "/client/" + activity.userId);
                System.out.println(url);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                int userID = -1;

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String output = br.readLine();
                    user = new JSONObject(output);
                    userID = user.getInt("id");
                    isPremium = user.getBoolean("is_Premium");
                    System.out.println("USER: " + user);

                    if (user.has("basicWorkout")) {
                        isAdvancedWorkout = false;
                        workout = user.getJSONObject("basicWorkout");
                        workoutDuration = workout.getString("duration");
                        workoutId = workout.getString("id");
                        setBasicWorkout();
                    } else if (user.has("advancedWorkout")) {
                        isAdvancedWorkout = true;
                        workout = user.getJSONObject("advancedWorkout");
                        workoutDuration = workout.getString("duration");
                        workoutId = workout.getString("id");
                        setAdvancedWorkout();
                    }
                    br.close();
                } else {
                    System.out.println("COULD NOT FIND USER");
                    return null;
                }

                conn.disconnect();
                if (userID == -1) {
                    System.out.println("USER NOT EXISTS");
                    return null;
                }
            } catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        protected void setAdvancedWorkout(){
            try {
                String currentDailyId = "";
                JSONObject currentDaily;
                JSONArray currentDailyExercises = new JSONArray();
                url = new URL(StaticStrings.ipserver + "/dailyadvancedworkout/findByAdvancedWorkoutId/" + workoutId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String output = br.readLine();
                    JSONArray arr = new JSONArray(output);
                    Calendar c = Calendar.getInstance();
                    String dayName = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
                    int dayOfWeek = 0;
                    if (dayName.equals("Monday")) {
                        dayOfWeek = 1;
                    } else if (dayName.equals("Tuesday")) {
                        dayOfWeek = 2;
                    } else if (dayName.equals("Wednesday")) {
                        dayOfWeek = 3;
                    } else if (dayName.equals("Thursday")) {
                        dayOfWeek = 4;
                    } else if (dayName.equals("Friday")) {
                        dayOfWeek = 5;
                    } else if (dayName.equals("Saturday")) {
                        dayOfWeek = 6;
                    } else if (dayName.equals("Sunday")) {
                        dayOfWeek = 7;
                    }
                    for (int i = 0; i < arr.length(); i++) {
                        Integer weekDay = arr.getJSONObject(i).getInt("week_day");
                        if (dayOfWeek == weekDay) {
                            currentDailyExercises = arr.getJSONObject(i).getJSONArray("advancedExercises");
                        }
                    }
                    if(null!=currentDailyExercises && currentDailyExercises.length()>0) {

                        for (int i = 0; i < currentDailyExercises.length(); i++) {
                            exercicesNames.add(currentDailyExercises.getJSONObject(i).getString("exerciseName"));
                            dailyExercices.add(currentDailyExercises.getJSONObject(i));
                        }
                    }
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            setExerciceSpinnerData(rootView);
                        }
                    });

                    br.close();
                } else {
                    System.out.println("COULD NOT FIND THE Advanced_Workout");
                }
                conn.disconnect();
            }catch(Exception e){
                System.out.println("ERROR: Something went wrong");
                e.printStackTrace();
            }
        }

        protected void setBasicWorkout(){
            try {
                String currentDailyId = "";
                JSONObject currentDaily;
                JSONArray currentDailyExercises = new JSONArray();
                url = new URL(StaticStrings.ipserver + "/dailybasicworkout/findByBasicWorkoutId/" + workoutId);
                conn = (HttpURLConnection) url.openConnection();

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String output = br.readLine();
                    JSONArray arr = new JSONArray(output);
                    Calendar c = Calendar.getInstance();
                    String dayName = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
                    int dayOfWeek = 0;
                    if (dayName.equals("Monday")) {
                        dayOfWeek = 1;
                    } else if (dayName.equals("Tuesday")) {
                        dayOfWeek = 2;
                    } else if (dayName.equals("Wednesday")) {
                        dayOfWeek = 3;
                    } else if (dayName.equals("Thursday")) {
                        dayOfWeek = 4;
                    } else if (dayName.equals("Friday")) {
                        dayOfWeek = 5;
                    } else if (dayName.equals("Saturday")) {
                        dayOfWeek = 6;
                    } else if (dayName.equals("Sunday")) {
                        dayOfWeek = 7;
                    }
                    for (int i = 0; i < arr.length(); i++) {
                        Integer weekDay = arr.getJSONObject(i).getInt("week_day");
                        if (dayOfWeek == weekDay) {
                            currentDailyExercises = arr.getJSONObject(i).getJSONArray("basicExercises");
                        }
                    }
                    if(null!=currentDailyExercises && currentDailyExercises.length()>0) {

                        for (int i = 0; i < currentDailyExercises.length(); i++) {
                            exercicesNames.add(currentDailyExercises.getJSONObject(i).getString("exerciseName"));
                            dailyExercices.add(currentDailyExercises.getJSONObject(i));
                        }
                    }

                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            setExerciceSpinnerData(rootView);
                        }
                    });
                    br.close();
                } else {
                    System.out.println("COULD NOT FIND THE Basic_Workout");
                }
                conn.disconnect();
            }catch(Exception e){
                System.out.println("ERROR: Something went wrong");
                e.printStackTrace();
            }
        }
    }

    private class UrlConnectorGetProgress extends AsyncTask<Void,Void,Void> {


        @Override
        protected Void doInBackground(Void... params) {

            try {
                if(isAdvancedWorkout) {
                    url = new URL(StaticStrings.ipserver + "/advancedclienttracking/findByClientId/" + activity.userId);
                    conn = (HttpURLConnection) url.openConnection();

                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String output = br.readLine();
                        JSONArray arr = new JSONArray(output);
                        if (null != arr && arr.length() > 0) {

                            for (int i = 0; i < arr.length(); i++) {
                                dailyTrackers.add(arr.getJSONObject(i));
                            }
                        }
                        br.close();
                    }
                } else {
                    url = new URL(StaticStrings.ipserver + "/basicclienttracking/findByClientId/" + activity.userId);
                    conn = (HttpURLConnection) url.openConnection();

                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String output = br.readLine();
                        JSONArray arr = new JSONArray(output);
                        if (arr != null && arr.length() > 0) {

                            for (int i = 0; i < arr.length(); i++) {
                                dailyTrackers.add(arr.getJSONObject(i));
                                System.out.println("Ho faig");
                            }
                        }
                        br.close();
                    }
                }
                conn.disconnect();
            }catch(Exception e){
                System.out.println("ERROR: Something went wrong");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                setData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }
    }


    private class UrlConnectorPostTracker extends AsyncTask<Void,Void,Void> {


        @Override
        protected Void doInBackground(Void... params) {

            try {
                Calendar cal = Calendar. getInstance();
                Date date=cal. getTime();
                DateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy");
                String formattedDate=dateFormat.format(date);

                JSONObject trackedExercice = new JSONObject();
                for(JSONObject exercice : dailyExercices){
                    if(exercice.getString("exerciseName").equals(exercicesSpinner.getSelectedItem().toString())) trackedExercice = exercice;
                }

                if(isAdvancedWorkout) {
                    url = new URL(StaticStrings.ipserver + "/advancedclienttracking");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    setsValue = Integer.parseInt(setsSpinner.getSelectedItem().toString());
                    repsValue = Integer.parseInt(repsSpinner.getSelectedItem().toString());
                    String jsonString = new JSONObject()
                            .put("advancedExerciseId", trackedExercice)
                            .put("clientId", user)
                            .put("trackingSets", setsValue)
                            .put("repetitions", repsValue)
                            .put("kcal", trackedExercice.get("kcal"))
                            .put("date", formattedDate)
                            .toString();
                    System.out.println(jsonString);
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonString.getBytes());
                    os.flush();
                    os.close();
                } else {
                    url = new URL(StaticStrings.ipserver + "/basicclienttracking/");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    setsValue = Integer.parseInt(setsSpinner.getSelectedItem().toString());
                    repsValue = Integer.parseInt(repsSpinner.getSelectedItem().toString());
                    String jsonString = new JSONObject()
                            .put("basicExerciseId", trackedExercice)
                            .put("clientId", user)
                            .put("trackingSets", setsValue)
                            .put("repetitions", repsValue)
                            .put("kcal", trackedExercice.get("kcal"))
                            .put("date", formattedDate)
                            .toString();

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonString.getBytes());
                    os.flush();
                    os.close();
                }
                System.out.println("CONNECTION CODE: " + conn.getResponseCode());
                conn.disconnect();
            }catch(Exception e){
                System.out.println("ERROR: Something went wrong");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                setData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }
    }
}
