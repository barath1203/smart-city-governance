package com.smartcity.governance.model;

public class RatingRequest {
    private Integer rating;
    private String ratingComment;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getRatingComment() { return ratingComment; }
    public void setRatingComment(String ratingComment) { this.ratingComment = ratingComment; }
}