package com.igp.admin.models.marketPlace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.igp.admin.response.Status;
import com.sun.research.ws.wadl.Include;

import javax.xml.crypto.Data;

/**
 * Created by suditi on 22/1/18.
 */
@JsonDeserialize
public class GeneralUserResponseModel {

    @JsonProperty("status")
    private Status status;

    @JsonProperty("data")
    private AuthResponseModel data;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public AuthResponseModel getData() {
        return data;
    }

    public void setData(AuthResponseModel data) {
        this.data = data;
    }
}
