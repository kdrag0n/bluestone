package com.khronodragon.bluestone.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Akinator {
    private static final String BASE_URL = "http://api-en4.akinator.com/ws/";
    private static final String NEW_SESSION_URL = BASE_URL + "new_session?partner=1";

    private enum Answer {
        UNKNOWN(-1),
        YES(0),
        NO(1),
        IDK(2),
        PROBABLY(3),
        PROBABLY_NOT(4);

        private int id;

        Answer(int id) {
            this.id = id;
        }

        private static Answer from(String ans) {
            switch (ans.toLowerCase()) {
                case "yes":
                case "y":
                    return YES;
                case "no":
                case "n":
                    return NO;
                case "dontknow":
                case "d":
                    return IDK;
                case "probably":
                case "p":
                    return PROBABLY;
                case "probablynot":
                case "pn":
                    return PROBABLY_NOT;
                default:
                    return UNKNOWN;
            }
        }
    }

    public class Question {
        public final int id;
        public final String text;
        public final Answer[] answers;
        public final boolean last;

        private Question(JSONObject data) {
            JSONObject params = data.getJSONObject("parameters");
            if (params.has("step_information") && params.getJSONObject("step_information").length() > 0)
                params = params.getJSONObject("step_information");

            this.id = params.getInt("questionid");
            this.text = params.getString("question");

            JSONArray answersData = params.getJSONArray("answers");
            Answer[] answers = new Answer[answersData.length()];
            for (int i = 0; i < answersData.length(); i++) {
                JSONObject answerData = answersData.getJSONObject(i);
                answers[i] = Answer.from(answerData.getString("text"));
            }
            this.answers = answers;

            this.last = params.getInt("progression") == 100;
        }
    }
    private final OkHttpClient client = new OkHttpClient();

    private String session = "";
    private String signature = "";
    private int step;

    public Question nextQuestion() {
        try {
            Response response = client.newCall(new Request.Builder()
                    .get()
                    .url(NEW_SESSION_URL)
                    .build()).execute();
            if (!response.isSuccessful()) return null;
            JSONObject resp = new JSONObject(response.body().string());


        } catch (NullPointerException | IOException | JSONException ignored) {
            return null;
        }
    }
}
