package com.abel.sentinel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdsbAircraft {

    @JsonProperty("hex")
    private String hex;

    @JsonProperty("flight")
    private String flight;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("alt_baro")
    private Object altBaro; // can be a number or the string "ground"

    @JsonProperty("gs")
    private Double gs;

    @JsonProperty("track")
    private Double track;
}