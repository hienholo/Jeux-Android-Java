package com.iot.jeux_mobile.qcm;

public class Question {
    public int id;
    public String question;
    public String reponse;
    public String[] propositions;
    public String niveau;
    public String theme;

    public Question(int id, String question, String reponse, String[] propositions, String niveau, String theme) {
        this.id = id;
        this.question = question;
        this.reponse = reponse;
        this.propositions = propositions;
        this.niveau = niveau;
        this.theme = theme;
    }
}
