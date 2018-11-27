package com.mati.resultados;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.mati.resultados.Constants.EXTRA_AWAY_TEAM;
import static com.mati.resultados.Constants.EXTRA_DAY;
import static com.mati.resultados.Constants.EXTRA_LOCAL_TEAM;
import static com.mati.resultados.Constants.EXTRA_MATCH;
import static com.mati.resultados.Constants.EXTRA_MATCHDAY;
import static com.mati.resultados.Constants.GOAL_ACTION;
import static com.mati.resultados.Constants.LOCAL_TEAM;
import static com.mati.resultados.Constants.MATCH;
import static com.mati.resultados.Constants.MISSED_PENALTY_ACTION;
import static com.mati.resultados.Constants.OWN_GOAL_ACTION;
import static com.mati.resultados.Constants.PENALTY_GOAL_ACTION;
import static com.mati.resultados.Constants.SECOND_YELLOW_CARD_ACTION;
import static com.mati.resultados.Constants.SUBSTITUTION_BY_INJURY_ACTION;
import static com.mati.resultados.Constants.YELLOW_CARD_ACTION;

public class MatchActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefreshLayout;

    ConstraintLayout layout;

    TextView status;

    LinearLayout chronologyLayout;

    LinearLayout localGoalsLayout, awayGoalsLayout;
    LinearLayout localRedCardsLayout, awayRedCardsLayout;

    String localTeam, awayTeam;
    int localScore, awayScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this::loadData);
        swipeRefreshLayout.setRefreshing(true);

        layout = findViewById(R.id.layout);

        status = findViewById(R.id.status);

        chronologyLayout = findViewById(R.id.chronology);

        localGoalsLayout = findViewById(R.id.local_goals);
        awayGoalsLayout = findViewById(R.id.away_goals);

        localRedCardsLayout = findViewById(R.id.local_red_cards);
        awayRedCardsLayout = findViewById(R.id.away_red_cards);

        loadData();
    }

    void loadData() {
        layout.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Intent intent = getIntent();

                int matchday = intent.getIntExtra(EXTRA_MATCHDAY, 0);
                int match = intent.getIntExtra(EXTRA_MATCH, 0);

                String url = String.format(MATCH, matchday, match);
                JSONObject matchData = new JSONObject(Utils.getStringFromUrl(url));

                String day = intent.getStringExtra(EXTRA_DAY);
                String time = matchData.getString("Time");

                String date = getResources().getString(R.string.date, day, time);

                localTeam = intent.getStringExtra(EXTRA_LOCAL_TEAM);
                awayTeam = intent.getStringExtra(EXTRA_AWAY_TEAM);

                String referee = matchData.getString("Referee");

                if (matchData.has("Transmission")) {
                    TextView transmissionTextView = findViewById(R.id.transmission);
                    String transmission = matchData.getString("Transmission");
                    runOnUiThread(() -> transmissionTextView.setText(transmission));

                    LinearLayout transmissionWrapper = findViewById(R.id.transmission_wrapper);
                    transmissionWrapper.setVisibility(View.VISIBLE);
                }

                TextView dateTextView = findViewById(R.id.date);
                TextView localTeamTextView = findViewById(R.id.local_team);
                TextView awayTeamTextView = findViewById(R.id.away_team);
                TextView refereeTextView = findViewById(R.id.referee);

                runOnUiThread(() -> {
                    dateTextView.setText(date);
                    localTeamTextView.setText(localTeam);
                    awayTeamTextView.setText(awayTeam);
                    refereeTextView.setText(referee);
                });

                if (matchData.has("Status"))
                    updateChronology(matchData);
                else {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        layout.setVisibility(View.VISIBLE);
                    });
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    void updateChronology(JSONObject data) throws JSONException {
        JSONArray statusArray = data.getJSONArray("Status");

        final String statusText = statusArray.getString(1);

        runOnUiThread(() -> status.setText(statusText));

        final int color = statusArray.getInt(0) == 1
                ? getResources().getColor(R.color.live)
                : Color.BLACK;

        if (statusArray.getInt(0) == 0)
            runOnUiThread(() -> status.setAlpha(0.87f));

        runOnUiThread(() -> status.setTextColor(color));

        if (data.has("Chronology")) {
            runOnUiThread(() -> {
                chronologyLayout.removeAllViews();

                localGoalsLayout.removeAllViews();
                awayGoalsLayout.removeAllViews();

                localRedCardsLayout.removeAllViews();
                awayRedCardsLayout.removeAllViews();
            });

            JSONArray chronology = data.getJSONArray("Chronology");

            LinkedHashMap<String, ArrayList<String>> localGoals = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> awayGoals = new LinkedHashMap<>();

            ArrayList<String> localRedCards = new ArrayList<>();
            ArrayList<String> awayRedCards = new ArrayList<>();

            for (int i = 0; i < chronology.length(); i++) {
                JSONArray event = chronology.getJSONArray(i);

                int team = event.getInt(1);
                int action = event.getInt(2);

                boolean isGoal = action >= GOAL_ACTION && action <= PENALTY_GOAL_ACTION;
                boolean isRedCard = action >= SECOND_YELLOW_CARD_ACTION;

                String minute = event.getString(0);
                String player = event.getString(3);

                if (isGoal) {
                    LinkedHashMap<String, ArrayList<String>> goals = team == LOCAL_TEAM
                            ? localGoals : awayGoals;

                    if (!goals.containsKey(player))
                        goals.put(player, new ArrayList<>());

                    ArrayList<String> playerGoals = goals.get(player);

                    if (action == PENALTY_GOAL_ACTION)
                        minute = getResources().getString(R.string.penalty_goal_summary, minute);
                    else if (action == OWN_GOAL_ACTION)
                        minute = getResources().getString(R.string.own_goal_summary, minute);
                    else
                        minute = getResources().getString(R.string.goal_summary, minute);

                    if (playerGoals != null) {
                        goals.put(player, playerGoals);
                        playerGoals.add(minute);
                    }
                }
                else if (isRedCard) {
                    ArrayList<String> redCards = team == LOCAL_TEAM ? localRedCards : awayRedCards;
                    String summary = getResources().getString(R.string.red_card_summary, player, minute);
                    redCards.add(summary);
                }

                if (isGoal) {
                    if (team == LOCAL_TEAM)
                        localScore++;
                    else
                        awayScore++;
                }
            }

            for (int i = 0; i < 2; i++) {
                LinkedHashMap<String, ArrayList<String>> goals = i == 0 ? localGoals : awayGoals;
                LinearLayout goalsTextView = i == 0 ? localGoalsLayout : awayGoalsLayout;

                for (LinkedHashMap.Entry<String, ArrayList<String>> entry : goals.entrySet()) {
                    String player = entry.getKey();
                    ArrayList<String> minutes = entry.getValue();

                    StringBuilder text = new StringBuilder(player + " " + minutes.get(0));

                    for (int j = 1; j < minutes.size(); j++)
                        text.append(", ").append(minutes.get(j));

                    TextView goal = (TextView) getLayoutInflater().inflate(R.layout.summary_item, goalsTextView, false);
                    goal.setText(text);
                    runOnUiThread(() -> goalsTextView.addView(goal));
                }
            }

            for (int i = 0; i < 2; i++) {
                ArrayList<String> redCards = i == 0 ? localRedCards : awayRedCards;
                LinearLayout redCardsTextView = i == 0 ? localRedCardsLayout : awayRedCardsLayout;

                for (String redCard : redCards) {
                    TextView goal = (TextView) getLayoutInflater().inflate(R.layout.summary_item, redCardsTextView, false);
                    goal.setText(redCard);
                    runOnUiThread(() -> redCardsTextView.addView(goal));
                }
            }

            boolean summaryAvailable = false;

            if (!localGoals.isEmpty() || !awayGoals.isEmpty()) {
                summaryAvailable = true;
                ConstraintLayout goalsWrapper = findViewById(R.id.goals_wrapper);
                goalsWrapper.setVisibility(View.VISIBLE);
            }

            if (!localRedCards.isEmpty() || !awayRedCards.isEmpty()) {
                summaryAvailable = true;
                ConstraintLayout redCardsWrapper = findViewById(R.id.red_cards_wrapper);
                redCardsWrapper.setVisibility(View.VISIBLE);
            }

            if (summaryAvailable) {
                final LinearLayout summary = findViewById(R.id.summary);
                runOnUiThread(() -> summary.setVisibility(View.VISIBLE));
            }

            TextView hyphen = findViewById(R.id.hyphen);
            hyphen.setVisibility(View.VISIBLE);

            TextView localScoreTextView = findViewById(R.id.local_score);
            localScoreTextView.setVisibility(View.VISIBLE);

            TextView awayScoreTextView = findViewById(R.id.away_score);
            awayScoreTextView.setVisibility(View.VISIBLE);

            runOnUiThread(() -> {
                localScoreTextView.setText(String.valueOf(localScore));
                awayScoreTextView.setText(String.valueOf(awayScore));
            });

            for (int i = chronology.length() - 1; i >= 0; i--) {
                JSONArray event = chronology.getJSONArray(i);

                String minute = event.getString(0);

                int team = event.getInt(1);
                int action = event.getInt(2);

                String teamName = (action == OWN_GOAL_ACTION ? 1 - team : team) == LOCAL_TEAM ? localTeam : awayTeam;

                int layout;

                if (action <= SUBSTITUTION_BY_INJURY_ACTION)
                    layout = R.layout.substitution_card;
                else if (action <= PENALTY_GOAL_ACTION)
                    layout = R.layout.goal_card;
                else
                    layout = R.layout.simple_action_card;

                View card = getLayoutInflater().inflate(layout, chronologyLayout, false);

                String minutes = getResources().getString(R.string.minute, minute);

                TextView minutesTextView = card.findViewById(R.id.minutes);
                minutesTextView.setText(minutes);

                ArrayList<TextView> players = new ArrayList<>();
                ArrayList<TextView> teams = new ArrayList<>();

                if (action <= SUBSTITUTION_BY_INJURY_ACTION) {
                    TextView playerIn = card.findViewById(R.id.player_in);
                    players.add(playerIn);

                    TextView playerOut = card.findViewById(R.id.player_out);
                    players.add(playerOut);

                    TextView teamIn = card.findViewById(R.id.team_in);
                    teams.add(teamIn);

                    TextView teamOut = card.findViewById(R.id.team_out);
                    teams.add(teamOut);
                }
                else {
                    TextView player = card.findViewById(R.id.player);
                    players.add(player);

                    TextView teamTextView = card.findViewById(R.id.team);
                    teams.add(teamTextView);

                    int imageResource, titleResource;

                    ImageView image = card.findViewById(R.id.image);
                    TextView title = card.findViewById(R.id.title);

                    if (action <= PENALTY_GOAL_ACTION) {
                        if (action == OWN_GOAL_ACTION) {
                            imageResource = R.drawable.ic_own_goal;
                            titleResource = R.string.own_goal;
                        }
                        else {
                            imageResource = R.drawable.ic_goal;
                            titleResource = R.string.goal;
                        }

                        TextView localTeamTextView = card.findViewById(R.id.local_team);
                        localTeamTextView.setText(localTeam);

                        TextView currentLocalScoreTextView = card.findViewById(R.id.local_score);
                        currentLocalScoreTextView.setText(String.valueOf(localScore));

                        TextView awayTeamTextView = card.findViewById(R.id.away_team);
                        awayTeamTextView.setText(awayTeam);

                        TextView currentAwayScoreTextView = card.findViewById(R.id.away_score);
                        currentAwayScoreTextView.setText(String.valueOf(awayScore));

                        if (team == 0) {
                            localTeamTextView.setTextColor(Color.BLACK);
                            currentLocalScoreTextView.setTextColor(Color.BLACK);
                        } else {
                            awayTeamTextView.setTextColor(Color.BLACK);
                            currentAwayScoreTextView.setTextColor(Color.BLACK);
                        }

                        if (action > GOAL_ACTION) {
                            TextView extra = card.findViewById(R.id.extra);
                            extra.setVisibility(View.VISIBLE);

                            int extraText, extraColor;

                            if (action == OWN_GOAL_ACTION) {
                                extraText = R.string.own_goal_extra;
                                extraColor = R.color.own_goal;
                            }
                            else {
                                extraText = R.string.penalty_goal_extra;
                                extraColor = R.color.penalty_goal;
                            }

                            extra.setText(extraText);
                            extra.setTextColor(getResources().getColor(extraColor));
                        }

                        if (team == 0)
                            localScore--;
                        else
                            awayScore--;
                    } else {
                        if (action == MISSED_PENALTY_ACTION) {
                            imageResource = R.drawable.ic_missed_penalty;
                            titleResource = R.string.missed_penalty;
                        } else if (action == YELLOW_CARD_ACTION) {
                            imageResource = R.drawable.ic_yellow_card;
                            titleResource = R.string.yellow_card;
                        }
                        else {
                            imageResource = action == SECOND_YELLOW_CARD_ACTION ?
                                    R.drawable.ic_second_yellow_card : R.drawable.ic_red_card;
                            titleResource = R.string.red_card;
                        }
                    }

                    image.setImageResource(imageResource);
                    title.setText(titleResource);
                }

                for (int j = 0; j < players.size(); j++) {
                    int initialIndex = 3 * (j + 1);

                    String player = event.getString(initialIndex);
                    players.get(j).setText(player);

                    int position = event.getInt(initialIndex + 1);
                    String positionText = getResources().getStringArray(R.array.positions)[position];

                    int number = event.getInt(initialIndex + 2);

                    String description = getResources().getString(R.string.player_extra, teamName, positionText, number);
                    teams.get(j).setText(description);
                }

                chronologyLayout.addView(card);
            }
        }

        runOnUiThread(() -> {
            swipeRefreshLayout.setRefreshing(false);
            layout.setVisibility(View.VISIBLE);
        });
    }
}
