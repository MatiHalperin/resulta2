package com.mati.resultados;

class Constants {
    private static final String API = "https://resulta2.herokuapp.com/";

    static final String FIXTURE = API + "fixture";
    static final String SPECIFIC_FIXTURE = FIXTURE + "/%s";
    static final String MATCH = SPECIFIC_FIXTURE + "/matches/%s";

    static final int LOCAL_TEAM = 0;

    //static final int SUBSTITUTION_ACTION = 0;
    static final int SUBSTITUTION_BY_INJURY_ACTION = 1;
    static final int GOAL_ACTION = 2;
    static final int OWN_GOAL_ACTION = 3;
    static final int PENALTY_GOAL_ACTION = 4;
    static final int MISSED_PENALTY_ACTION = 5;
    static final int YELLOW_CARD_ACTION = 6;
    static final int SECOND_YELLOW_CARD_ACTION = 7;
    //static final int RED_CARD_ACTION = 8;

    static final String EXTRA_MATCHDAY = "com.mati.resultados.MATCHDAY";
    static final String EXTRA_MATCH = "com.mati.resultados.MATCH";

    static final String EXTRA_LOCAL_TEAM = "com.mati.resultados.LOCAL_TEAM";
    static final String EXTRA_AWAY_TEAM = "com.mati.resultados.AWAY_TEAM";

    static final String EXTRA_DAY = "com.mati.resultados.DAY";
}
