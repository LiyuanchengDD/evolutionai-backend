package com.example.grpcdemo.controller.dto;

/**
 * 单题作答后的反馈。
 */
public class CandidateInterviewAnswerResponse {

    private CandidateInterviewRecordResponse record;
    private CandidateInterviewAudioDto audio;

    public CandidateInterviewRecordResponse getRecord() {
        return record;
    }

    public void setRecord(CandidateInterviewRecordResponse record) {
        this.record = record;
    }

    public CandidateInterviewAudioDto getAudio() {
        return audio;
    }

    public void setAudio(CandidateInterviewAudioDto audio) {
        this.audio = audio;
    }
}

