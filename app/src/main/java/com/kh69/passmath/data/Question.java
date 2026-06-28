package com.kh69.passmath.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


@Entity(tableName = "questions")
public class Question {
    @PrimaryKey
    @ColumnInfo(name = "questionId")
    @SerializedName("_id")
    @Expose
    @NonNull
    private String id;

    @SerializedName("qtn_text")
    @Expose
    private String text;

    @SerializedName("year")
    @Expose
    private int year;

    @SerializedName("paper")
    @Expose
    private int paper;

    @SerializedName("section")
    @Expose
    private String section;

    @SerializedName("topic")
    @Expose
    private String topic;

    @SerializedName("answer")
    @Expose
    private String answer;

    @SerializedName("katex_question")
    @Expose
    private String katex_question;

    @SerializedName("katex_answer")
    @Expose
    private String katex_answer;

    @SerializedName("edited")
    @Expose
    private boolean edited;

    /**
     * No-arg constructor for Gson deserialization (the bundled JSON asset is parsed into
     * Question objects on first launch). Annotated {@link Ignore} so Room keeps using the
     * full-field constructor below.
     */
    @Ignore
    public Question() {
    }

    public Question(String text, int year, int paper, String section, String topic, String answer, String katex_question, String katex_answer, boolean edited) {
        this.text           = text;
        this.year           = year;
        this.paper          = paper;
        this.section        = section;
        this.topic          = topic;
        this.answer         = answer;
        this.katex_question = katex_question;
        this.katex_answer   = katex_answer;
        this.edited         = edited;
    }

    public boolean getEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getPaper() {
        return paper;
    }

    public void setPaper(int paper) {
        this.paper = paper;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getKatex_question() {
        return katex_question;
    }

    public void setKatex_question(String katex_question) {
        this.katex_question = katex_question;
    }

    public String getKatex_answer() {
        return katex_answer;
    }

    public void setKatex_answer(String katex_answer) {
        this.katex_answer = katex_answer;
    }
}
