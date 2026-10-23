package com.queuesense.dto.analytics;

public class ForecastHorizonDto {

    private int horizonMinutes;
    private String targetTimestamp;
    private double predictedOccupancy;
    private Double predictedOccupancyPercentage;
    private String predictedCrowdLevel;
    private String confidenceNote;

    public ForecastHorizonDto() {}

    public ForecastHorizonDto(int horizonMinutes, String targetTimestamp, double predictedOccupancy,
                              Double predictedOccupancyPercentage, String predictedCrowdLevel, String confidenceNote) {
        this.horizonMinutes = horizonMinutes;
        this.targetTimestamp = targetTimestamp;
        this.predictedOccupancy = predictedOccupancy;
        this.predictedOccupancyPercentage = predictedOccupancyPercentage;
        this.predictedCrowdLevel = predictedCrowdLevel;
        this.confidenceNote = confidenceNote;
    }

    public int getHorizonMinutes() { return horizonMinutes; }
    public void setHorizonMinutes(int horizonMinutes) { this.horizonMinutes = horizonMinutes; }

    public String getTargetTimestamp() { return targetTimestamp; }
    public void setTargetTimestamp(String targetTimestamp) { this.targetTimestamp = targetTimestamp; }

    public double getPredictedOccupancy() { return predictedOccupancy; }
    public void setPredictedOccupancy(double predictedOccupancy) { this.predictedOccupancy = predictedOccupancy; }

    public Double getPredictedOccupancyPercentage() { return predictedOccupancyPercentage; }
    public void setPredictedOccupancyPercentage(Double predictedOccupancyPercentage) { this.predictedOccupancyPercentage = predictedOccupancyPercentage; }

    public String getPredictedCrowdLevel() { return predictedCrowdLevel; }
    public void setPredictedCrowdLevel(String predictedCrowdLevel) { this.predictedCrowdLevel = predictedCrowdLevel; }

    public String getConfidenceNote() { return confidenceNote; }
    public void setConfidenceNote(String confidenceNote) { this.confidenceNote = confidenceNote; }
}
