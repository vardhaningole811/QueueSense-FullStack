package com.queuesense.dto.analytics;

import java.util.List;

public class ModelTransparencyDto {

    private boolean isPrototype = true;
    private String dataSource;
    private String modelSelected;
    private Double validationMae;
    private Double validationRmse;
    private Double baselineMae;
    private Double baselineRmse;
    private List<Integer> horizons;
    private String disclaimer;
    private String trainingTimestamp;
    private Integer sampleSize;

    public ModelTransparencyDto() {}

    public ModelTransparencyDto(boolean isPrototype, String dataSource, String modelSelected,
                                Double validationMae, Double validationRmse,
                                Double baselineMae, Double baselineRmse,
                                List<Integer> horizons, String disclaimer,
                                String trainingTimestamp, Integer sampleSize) {
        this.isPrototype = isPrototype;
        this.dataSource = dataSource;
        this.modelSelected = modelSelected;
        this.validationMae = validationMae;
        this.validationRmse = validationRmse;
        this.baselineMae = baselineMae;
        this.baselineRmse = baselineRmse;
        this.horizons = horizons;
        this.disclaimer = disclaimer;
        this.trainingTimestamp = trainingTimestamp;
        this.sampleSize = sampleSize;
    }

    public boolean isPrototype() { return isPrototype; }
    public void setPrototype(boolean prototype) { isPrototype = prototype; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getModelSelected() { return modelSelected; }
    public void setModelSelected(String modelSelected) { this.modelSelected = modelSelected; }

    public Double getValidationMae() { return validationMae; }
    public void setValidationMae(Double validationMae) { this.validationMae = validationMae; }

    public Double getValidationRmse() { return validationRmse; }
    public void setValidationRmse(Double validationRmse) { this.validationRmse = validationRmse; }

    public Double getBaselineMae() { return baselineMae; }
    public void setBaselineMae(Double baselineMae) { this.baselineMae = baselineMae; }

    public Double getBaselineRmse() { return baselineRmse; }
    public void setBaselineRmse(Double baselineRmse) { this.baselineRmse = baselineRmse; }

    public List<Integer> getHorizons() { return horizons; }
    public void setHorizons(List<Integer> horizons) { this.horizons = horizons; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public String getTrainingTimestamp() { return trainingTimestamp; }
    public void setTrainingTimestamp(String trainingTimestamp) { this.trainingTimestamp = trainingTimestamp; }

    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer sampleSize) { this.sampleSize = sampleSize; }
}
