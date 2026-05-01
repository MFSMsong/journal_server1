package com.uuorb.journal.controller.vo;

public class UploadCredential {
    private String uploadUrl;
    private String cosPath;

    public UploadCredential() {
    }

    public UploadCredential(String uploadUrl, String cosPath) {
        this.uploadUrl = uploadUrl;
        this.cosPath = cosPath;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getCosPath() {
        return cosPath;
    }

    public void setCosPath(String cosPath) {
        this.cosPath = cosPath;
    }
}
