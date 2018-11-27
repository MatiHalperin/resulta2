package com.mati.resultados;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class FixtureActivity extends AppCompatActivity {

    Context context;

    ConstraintLayout headerLayout;
    Spinner spinner;

    SwipeRefreshLayout fixtureLayout;
    LinearLayout matchesLayout;

    int matchday, matchDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixture);

        context = this;

        new Thread(() -> {
            headerLayout = findViewById(R.id.time_wrapper);

            spinner = findViewById(R.id.match_days);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    matchday = matchdayStringToNumber(selectedItem);

                    ImageView arrowBack = findViewById(R.id.arrow_back);
                    arrowBack.setVisibility(matchday == 1 ? View.GONE : View.VISIBLE);

                    ImageView arrowForward = findViewById(R.id.arrow_forward);
                    arrowForward.setVisibility(matchday == matchDays ? View.GONE : View.VISIBLE);

                    if (spinner.getTag() != null)
                        loadData();
                    else
                        spinner.setTag("1");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });

            fixtureLayout = findViewById(R.id.fixture);
            fixtureLayout.setColorSchemeResources(R.color.colorPrimary);
            fixtureLayout.setOnRefreshListener(this::loadData);

            matchesLayout = findViewById(R.id.matches);

            firstDataLoad();
        }).start();
    }

    void firstDataLoad() {
        try {
            fixtureLayout.setRefreshing(true);

            headerLayout.setVisibility(View.GONE);

            String url = Constants.FIXTURE;
            JSONObject data = new JSONObject(Utils.getStringFromUrl(url));

            matchday = data.getInt("CurrentMatchday");
            matchDays = data.getInt("MatchDays");

            ArrayList<String> stringArrayList = new ArrayList<>();

            for (int i = 1; i <= matchDays; i++)
                stringArrayList.add(getResources().getString(R.string.matchday, i));

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, stringArrayList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            runOnUiThread(() -> {
                spinner.setAdapter(adapter);
                spinner.setSelection(matchday - 1);
            });

            JSONArray matches = data.getJSONArray("Matches");

            showData(matches);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    void loadData() {
        fixtureLayout.setRefreshing(true);

        new Thread(() -> {
            try {
                String url = String.format(Constants.SPECIFIC_FIXTURE, matchday);
                JSONArray matches = new JSONArray(Utils.getStringFromUrl(url));
                showData(matches);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    void showData(JSONArray matches) throws JSONException {
        runOnUiThread(() -> {
            matchesLayout.setVisibility(View.GONE);
            matchesLayout.removeAllViews();
        });

        int match = 0;

        for (int i = 0; i < matches.length(); i++) {
            final String day = matches.getJSONArray(i).getString(0);

            final TextView dayTextView = (TextView) getLayoutInflater().inflate(R.layout.day, matchesLayout, false);
            dayTextView.setText(day);

            runOnUiThread(() -> matchesLayout.addView(dayTextView));

            for (int j = 1; j < matches.getJSONArray(i).length(); j++) {
                match++;

                final JSONArray data = matches.getJSONArray(i).getJSONArray(j);

                final CardView card = (CardView) getLayoutInflater().inflate(R.layout.match_card, matchesLayout, false);

                int localTeamPosition = -1;
                int awayTeamPosition = -1;

                if (data.length() == 4) {
                    localTeamPosition = 2;
                    awayTeamPosition = 3;
                }
                else if (data.length() == 8) {
                    localTeamPosition = 2;
                    awayTeamPosition = 5;
                }

                final String localTeam = data.getString(localTeamPosition);
                final String awayTeam = data.getString(awayTeamPosition);

                TextView localTeamTextView = card.findViewById(R.id.local_team);
                localTeamTextView.setText(localTeam);

                TextView awayTeamTextView = card.findViewById(R.id.away_team);
                awayTeamTextView.setText(awayTeam);

                if (data.length() == 8) {
                    for (int k = 0; k < data.getInt(4); k++) {
                        LinearLayout localTeamWrapper = card.findViewById(R.id.local_team_wrapper);
                        getLayoutInflater().inflate(R.layout.red_card, localTeamWrapper);
                    }

                    for (int k = 0; k < data.getInt(7); k++) {
                        LinearLayout awayTeamWrapper = card.findViewById(R.id.away_team_wrapper);
                        getLayoutInflater().inflate(R.layout.red_card, awayTeamWrapper);
                    }

                    int localScore = data.getInt(3);
                    int awayScore = data.getInt(6);

                    TextView localScoreTextView = card.findViewById(R.id.local_score);
                    localScoreTextView.setText(String.valueOf(localScore));

                    TextView awayScoreTextView = card.findViewById(R.id.away_score);
                    awayScoreTextView.setText(String.valueOf(awayScore));

                    if (localScore >= awayScore) {
                        localTeamTextView.setTextColor(Color.BLACK);
                        localScoreTextView.setTextColor(Color.BLACK);
                    }

                    if (awayScore >= localScore) {
                        awayTeamTextView.setTextColor(Color.BLACK);
                        awayScoreTextView.setTextColor(Color.BLACK);
                    }
                }

                String time = data.getString(1);

                TextView timeTextView = card.findViewById(R.id.time);
                timeTextView.setText(time);

                if (data.getInt(0) == 1)
                    timeTextView.setTextColor(getResources().getColor(R.color.live));

                final int finalMatch = match;

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(context, MatchActivity.class);

                    intent.putExtra(Constants.EXTRA_MATCHDAY, matchday);
                    intent.putExtra(Constants.EXTRA_MATCH, finalMatch);

                    intent.putExtra(Constants.EXTRA_LOCAL_TEAM, localTeam);
                    intent.putExtra(Constants.EXTRA_AWAY_TEAM, awayTeam);

                    intent.putExtra(Constants.EXTRA_DAY, day);

                    startActivity(intent);
                });

                runOnUiThread(() -> matchesLayout.addView(card));
            }
        }

        runOnUiThread(() -> {
            fixtureLayout.setRefreshing(false);
            headerLayout.setVisibility(View.VISIBLE);
            matchesLayout.setVisibility(View.VISIBLE);
        });
    }

    public void arrowClick(View view) {
        Spinner spinner = findViewById(R.id.match_days);

        int selectedMatchday = matchdayStringToNumber(spinner.getSelectedItem().toString());
        int difference = Integer.valueOf(view.getTag().toString());

        spinner.setSelection(selectedMatchday + difference - 1);
    }

    int matchdayStringToNumber(String matchday) {
        return Integer.valueOf(matchday.substring(matchday.lastIndexOf(' ') + 1));
    }
}
