# QueueSense — Machine Learning Crowd Analytics Module

> **Component**: ML-Based Physical Crowd Density & Time-Series Forecasting Engine  
> **Status**: `PROTOTYPE — REAL-WORLD VALIDATION PENDING`  
> **Target Facility**: Campus E-Library (Architected to support all campus spaces)  

---

## 1. Overview & Project Role

This module provides predictive crowd analytics for QueueSense:
- **Real-Time Digital Occupancy**: Aggregates verified active usage sessions from PostgreSQL, strictly excluding maintenance desks from capacity.
- **Multi-Horizon Forecasting**: Predicts physical attendance +15 minutes, +30 minutes, and +60 minutes ahead using scikit-learn regression models.
- **Smart Decision Support**: Recommends optimal study visit windows and calculates estimated time until full capacity.
- **Zero Target Leakage**: Engineered features use strictly past observations (lags and rolling statistics).
- **Dual Data Paths**: Isolated synthetic dataset for prototype demonstration, and a clean CSV template pipeline for future real physical observations.

---

## 2. Directory Structure

```
ml/
├── data/
│   ├── templates/
│   │   ├── crowd_observation_template.csv     # Empty manual physical headcount collection template
│   │   └── crowd_observation_sample.csv       # Illustrative sample rows ([SAMPLE_ONLY] tags)
│   ├── synthetic/
│   │   └── synthetic_elibrary_observations.csv # 1,568 simulated observations (28 operating days)
│   └── real/                                   # Destination for validated campus CSV observations
├── models/
│   ├── elibrary_forecast_models.joblib        # Serialized best regression model bundle
│   └── model_metadata.json                    # Audit comparison table (MAE/RMSE for all models vs baseline)
├── src/
│   ├── config.py                              # Horizons, feature lists, operating hours
│   ├── synthetic_generator.py                 # Time-series physical occupancy simulation
│   ├── data_pipeline.py                       # Ingestion, validation, cleaning, lag feature engineering
│   ├── train.py                               # Chronological split, 4 regression models vs naive baseline
│   ├── predict.py                             # Inference engine (+15, +30, +60 forecasts, crowd level)
│   └── service.py                             # Lightweight Flask microservice (port 5001)
├── venv/                                      # Isolated Python virtual environment (ignored by Git)
├── requirements.txt                           # Frozen package dependencies
├── test_ml.py                                 # Unit & integration test suite
└── README.md                                  # This documentation
```

---

## 3. Quickstart & Setup

### A. Environment Initialization
```powershell
# Create isolated virtual environment
python -m venv ml/venv

# Activate virtual environment
.\ml\venv\Scripts\Activate.ps1

# Install required dependencies
pip install -r ml/requirements.txt
```

### B. Generate Synthetic Prototype Data
```powershell
& "ml\venv\Scripts\python.exe" ml\src\synthetic_generator.py
```

### C. Train and Compare Regression Models
```powershell
& "ml\venv\Scripts\python.exe" ml\src\train.py
```

### D. Run Unit & Pipeline Tests
```powershell
& "ml\venv\Scripts\python.exe" ml\test_ml.py
```

### E. Start the ML Inference Microservice
```powershell
& "ml\venv\Scripts\python.exe" ml\src\service.py
```
*The service listens on `http://localhost:5001`. Health check is available at `http://localhost:5001/health`.*

---

## 4. Models Compared & Syllabus Alignment

In academic vivas and project presentations, 4 syllabus-friendly regression algorithms are evaluated against a **Naive Persistence Baseline**:

1. **Naive Persistence Baseline**: Predicts that future occupancy at $t+h$ equals the current observed occupancy ($t$). Serves as the benchmark to prove machine learning adds predictive value.
2. **Linear Regression**: Fast, highly interpretable parametric model establishing baseline linear feature correlations.
3. **Decision Tree Regressor**: Non-linear rule-based tree (`max_depth=5`) capturing step changes in attendance.
4. **Random Forest Regressor**: Ensemble bagging technique (`n_estimators=50, max_depth=6`) reducing tree variance.
5. **Gradient Boosting Regressor**: Sequential boosting algorithm (`n_estimators=50, max_depth=4, lr=0.08`) optimizing residual errors.

### Chronological Splitting (Zero Information Leakage)
Unlike random k-fold cross-validation which causes time-series data leakage, observations are partitioned strictly chronologically:
- **First 70%**: Training set
- **Next 15%**: Validation set (used for model selection)
- **Final 15%**: Holdout test set

### Evaluation Metrics
- **Mean Absolute Error (MAE)**: $\frac{1}{N}\sum |y_i - \hat{y}_i|$ — Average deviation in headcount (people).
- **Root Mean Squared Error (RMSE)**: $\sqrt{\frac{1}{N}\sum (y_i - \hat{y}_i)^2}$ — Penalizes large unexpected crowd spikes.

---

## 5. Transitioning from Prototype to Real Observations

1. Record manual counts every 15 minutes in `ml/data/templates/crowd_observation_template.csv`.
2. Save the completed file into `ml/data/real/` (e.g. `ml/data/real/library_october_2026.csv`).
3. Re-run training:
   ```powershell
   & "ml\venv\Scripts\python.exe" ml\src\train.py
   ```
4. The pipeline automatically detects the real dataset, validates timestamps, removes duplicates, trains the models, and updates `model_metadata.json` with `is_prototype: false`.
